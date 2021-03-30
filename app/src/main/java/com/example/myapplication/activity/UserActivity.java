package com.example.myapplication.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;
import com.example.myapplication.document.UsersDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Converter;
import com.example.myapplication.utilities.Formatter;
import com.example.myapplication.utilities.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

public class UserActivity extends BaseActivity implements DatePickerDialog.OnDateSetListener {
    public static final String TAG = "UserActivity";

    private ImageView mAvatarImageView;
    private FloatingActionButton mChooseImageButton;
    private FloatingActionButton mTakePhotoButton;
    private TextInputLayout mUsernameTextInputLayout;
    private TextInputEditText mUsernameEditText;
    private TextInputLayout mDobTextInputLayout;
    private TextInputEditText mDobEditText;
    private TextInputEditText mBioEditText;
    private ProgressBar mProgressBar;
    private MaterialButton mSaveButton;

    private String mCurrentUid;
    private UsersDocument mUsersDocument;
    private boolean mUsernameNotSet;
    private boolean mAvatarHasChanged;
    private boolean mDocumentHasChanged;
    private ActivityResultLauncher<Intent> mCameraLauncher;
    private ActivityResultLauncher<Intent> mGetContentLauncher;
    private final ActivityResultLauncher<Intent> mActivityLauncher;

    public UserActivity() {
        mActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        setTitle(getString(R.string.activity_user_title));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        setReference();
        setDefault();
        loadDocumentRelatedInfo();
    }

    private void setReference() {
        mUsernameNotSet = getIntent().getBooleanExtra(Constants.EXTRA_USERNAME_NOT_SET, false);
        mCurrentUid = Utils.getCurrentUid();
        mAvatarImageView = findViewById(R.id.activity_user_image_view_avatar);
        mChooseImageButton = findViewById(R.id.activity_user_button_choose_image);
        mTakePhotoButton = findViewById(R.id.activity_user_button_take_photo);
        mUsernameTextInputLayout = findViewById(R.id.activity_user_text_input_layout_username);
        mUsernameEditText = findViewById(R.id.activity_user_edit_text_username);
        mDobTextInputLayout = findViewById(R.id.activity_user_text_input_layout_dob);
        mDobEditText = findViewById(R.id.activity_user_edit_text_dob);
        mBioEditText = findViewById(R.id.activity_user_edit_text_bio);
        mProgressBar = findViewById(R.id.activity_user_progress_bar);
        mSaveButton = findViewById(R.id.activity_user_button_save);
        mCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onRequestOpenCameraReturn);
        mGetContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onGetImageLauncherReturn);
    }

    private void setDefault() {
        mDobEditText.setInputType(InputType.TYPE_NULL);
        mSaveButton.setText(mUsernameNotSet ? getString(R.string.activity_user_button_save_text_continue) : getString(R.string.activity_user_button_save_text_save));
    }

    private void setListener() {
        mAvatarImageView.setOnClickListener(v -> onAvatarImageViewClick());
        mChooseImageButton.setOnClickListener(v -> onChooseImageButtonClick());
        mTakePhotoButton.setOnClickListener(v -> onTakePhotoButtonClick());
        mDobEditText.setOnClickListener(v -> onDobEditTextClick());
        mSaveButton.setOnClickListener(v -> onSaveButtonClick());
        mUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String username = Formatter.formatName(s.toString());
                if (username.length() < Constants.NAME_LENGTH_MIN) {
                    mUsernameTextInputLayout.setError(getString(R.string.activity_user_text_input_layout_username_error_min, 5));
                } else if (username.length() > Constants.NAME_LENGTH_MAX) {
                    mUsernameTextInputLayout.setError(getString(R.string.activity_user_text_input_layout_username_error_max));
                } else {
                    mUsernameTextInputLayout.setError(null);
                    mDocumentHasChanged = mUsersDocument == null || !username.equals(mUsersDocument.getUsername());
                }
                checkSaveButtonEnable();
            }
        });
        mBioEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String bio = Formatter.formatName(mBioEditText.getEditableText().toString());
                mDocumentHasChanged = mUsersDocument == null || !bio.equals(mUsersDocument.getBio());
                checkSaveButtonEnable();
            }
        });
    }

    private void checkSaveButtonEnable() {
        mSaveButton.setEnabled(mAvatarHasChanged || mDocumentHasChanged);
    }

    private void loadDocumentRelatedInfo() {
        Utils.getUsersRef().document(mCurrentUid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Utils.logTaskException(TAG, task);
                return;
            }
            mUsersDocument = task.getResult().toObject(UsersDocument.class);
            if (mUsersDocument == null || mUsersDocument.getUsername().isEmpty()) {
                mUsernameTextInputLayout.setError(getString(R.string.activity_user_text_input_layout_username_error_lack));
            } else {
                mUsernameEditText.setText(mUsersDocument.getUsername());
                mDobEditText.setText(mUsersDocument.getDob());
                mBioEditText.setText(mUsersDocument.getBio());
            }
            loadFileRelatedInfo();
        });
    }

    private void loadFileRelatedInfo() {
        StorageReference avatarRef = Utils.getUsersAvatarRef(mCurrentUid);
        Glide.with(this).load(avatarRef).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        setListener();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        setListener();
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    private void onAvatarImageViewClick() {
        if (mChooseImageButton.getVisibility() == View.INVISIBLE
                && mTakePhotoButton.getVisibility() == View.INVISIBLE) {
            mChooseImageButton.setVisibility(View.VISIBLE);
            mTakePhotoButton.setVisibility(View.VISIBLE);
        } else {
            mChooseImageButton.setVisibility(View.INVISIBLE);
            mTakePhotoButton.setVisibility(View.INVISIBLE);
        }
    }

    private void onChooseImageButtonClick() {
        mChooseImageButton.setVisibility(View.INVISIBLE);
        mTakePhotoButton.setVisibility(View.INVISIBLE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        mGetContentLauncher.launch(Intent.createChooser(intent, ""));
    }

    private void onTakePhotoButtonClick() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA_PERMISSION_CODE);
        } else {
            mChooseImageButton.setVisibility(View.INVISIBLE);
            mTakePhotoButton.setVisibility(View.INVISIBLE);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mCameraLauncher.launch(cameraIntent);
        }
    }

    private void onDobEditTextClick() {
        mUsernameEditText.clearFocus();
        mBioEditText.clearFocus();
        if (mUsersDocument.getDob() == null) {
            Calendar calendar = new GregorianCalendar();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, this, year, month, dayOfMonth).show();
        } else {
            String[] dobArray = Objects.requireNonNull(mDobEditText.getText()).toString().split(Constants.DOB_SEPARATOR);
            int year = Integer.parseInt(dobArray[2]);
            int month = Integer.parseInt(dobArray[1]);
            int dayOfMonth = Integer.parseInt(dobArray[0]);
            new DatePickerDialog(this, this, year, month - 1, dayOfMonth).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Constants.REQUEST_CAMERA_PERMISSION_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onTakePhotoButtonClick();
            }
        }
    }

    private void onRequestOpenCameraReturn(@NonNull ActivityResult result) {
        Intent intent = result.getData();
        if (result.getResultCode() != RESULT_OK || intent == null) {
            return;
        }
        Bitmap bitmap = (Bitmap) intent.getExtras().get("data");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Glide.with(this).load(data).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mAvatarHasChanged = true;
                        checkSaveButtonEnable();
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    private void onGetImageLauncherReturn(@NonNull ActivityResult result) {
        if (result.getResultCode() == RESULT_CANCELED || result.getData() == null) {
            return;
        }
        Uri uri = result.getData().getData();
        Glide.with(this).load(uri).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mAvatarHasChanged = true;
                        checkSaveButtonEnable();
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    private void onSaveButtonClick() {
        if (mUsernameNotSet) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(getString(R.string.pref_username_set), true);
            editor.apply();
        }
        if (mAvatarHasChanged) {
            uploadAvatar();
        } else if (mDocumentHasChanged) {
            updateDocumentRelatedInfo();
        }
    }

    private void uploadAvatar() {
        Bitmap bitmap = ((BitmapDrawable) mAvatarImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Utils.getUsersAvatarRef(mCurrentUid).putBytes(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            if (mDocumentHasChanged) {
                updateDocumentRelatedInfo();
            } else {
                mSaveButton.setEnabled(false);
                Toast.makeText(UserActivity.this, getString(R.string.update_success), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateDocumentRelatedInfo() {
        String dob = mDobEditText.getEditableText().toString();
        String bio = Formatter.formatName(mBioEditText.getEditableText().toString());
        String username = Formatter.formatName(mUsernameEditText.getEditableText().toString());
        UsersDocument usersDocument = new UsersDocument();
        usersDocument.setUsername(username);
        usersDocument.setUsernameLowercase(username.toLowerCase());
        usersDocument.setDob(dob.isEmpty() ? null : dob);
        usersDocument.setBio(bio.isEmpty() ? null : bio);
        usersDocument.setStatus(Constants.STATUS_ONLINE);
        Utils.getUsersRef().document(mCurrentUid).set(usersDocument).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            if (mUsernameNotSet && mAvatarHasChanged) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(Constants.EXTRA_AUTHENTICATED, true);
                mActivityLauncher.launch(intent);
                finish();
            } else if (mUsernameNotSet) {
                uploadDefaultAvatar();
            } else {
                mSaveButton.setEnabled(false);
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadDefaultAvatar() {
        Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.ic_baseline_person_24);
        Bitmap bitmap = Converter.convertDrawableToBitmap(drawable);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Utils.getUsersAvatarRef(mCurrentUid).putBytes(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(Constants.EXTRA_AUTHENTICATED, true);
            mActivityLauncher.launch(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (android.R.id.home == itemId) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_user_root_layout);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar calendar = new GregorianCalendar();
        int thisYear = calendar.get(Calendar.YEAR);
        int thisMonth = calendar.get(Calendar.MONTH);
        int thisDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        if (year > thisYear || (year == thisYear && month > thisMonth)
                || (year == thisYear && month == thisMonth && dayOfMonth > thisDayOfMonth)) {
            mDobEditText.setText("");
            mDobTextInputLayout.setError(getString(R.string.error_invalid_dob));
        } else {
            mDobTextInputLayout.setError(null);
            String dob = getString(R.string.dob_format, dayOfMonth, month + 1, year);
            mDobEditText.setText(dob);
            mDocumentHasChanged = mUsersDocument == null || !dob.equals(mUsersDocument.getDob());
        }
        checkSaveButtonEnable();
    }
}