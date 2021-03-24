package com.example.myapplication.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
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
import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        setTitle(getString(R.string.activity_user_title));
        setReference();
        setDefault();
        loadData();
        setListener();
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
        mSaveButton.setEnabled(false);
        mSaveButton.setText(mUsernameNotSet ? getString(R.string.activity_user_button_save_text_continue) : getString(R.string.activity_user_button_save_text_save));
    }

    private void setListener() {
        mAvatarImageView.setOnClickListener(v -> onAvatarImageViewClick());
        mChooseImageButton.setOnClickListener(v -> onChooseImageButtonClick());
        mTakePhotoButton.setOnClickListener(v -> onTakePhotoButtonClick());
        mDobEditText.setOnClickListener(v -> onDobTextInputLayoutClick());
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
                    mSaveButton.setEnabled(false);
                } else if (username.length() > Constants.NAME_LENGTH_MAX) {
                    mUsernameTextInputLayout.setError(getString(R.string.activity_user_text_input_layout_username_error_max));
                    mSaveButton.setEnabled(false);
                } else if (username.contains("_")) {
                    mUsernameTextInputLayout.setError(getString(R.string.activity_user_text_input_layout_username_error_contain_underscore));
                    mSaveButton.setEnabled(false);
                } else {
                    mUsernameTextInputLayout.setError(null);
                    mDocumentHasChanged = !username.equals(mUsersDocument.getUsername());
                    checkSaveButtonEnable();
                }
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
                mDocumentHasChanged = !bio.equals(mUsersDocument.getBio());
                checkSaveButtonEnable();
            }
        });
    }

    private void checkSaveButtonEnable() {
        mSaveButton.setEnabled(mAvatarHasChanged || mDocumentHasChanged);
    }

    private void loadData() {
        loadFileRelatedInfo();
        loadDocumentRelatedInfo();
    }

    private void loadFileRelatedInfo() {
        StorageReference avatarRef = Utils.getUsersAvatarRef(mCurrentUid);
        Glide.with(this).load(avatarRef).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mAvatarImageView);
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
        });
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

    private void onDobTextInputLayoutClick() {
        mUsernameEditText.clearFocus();
        mBioEditText.clearFocus();
        Calendar calendar = new GregorianCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, this, year, month, dayOfMonth).show();
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
        mAvatarHasChanged = true;
        Bitmap bitmap = (Bitmap) intent.getExtras().get("data");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Glide.with(this).load(data).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mAvatarImageView);
    }

    private void onGetImageLauncherReturn(ActivityResult result) {
        if (result.getResultCode() == RESULT_CANCELED || result.getData() == null) {
            return;
        }
        Uri uri = result.getData().getData();
        Glide.with(this).load(uri).error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mAvatarImageView);
        mAvatarHasChanged = true;
        if (mUsernameTextInputLayout.getError() == null) {
            mSaveButton.setEnabled(true);
        }
    }

    private void onSaveButtonClick() {
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
        usersDocument.setDob(dob);
        usersDocument.setBio(bio);
        usersDocument.setStatus(Constants.STATUS_ONLINE);
        Utils.getUsersRef().document(mCurrentUid).set(usersDocument).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            if (mUsernameNotSet && mAvatarHasChanged) {
                startActivity(new Intent(this, MainActivity.class));
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
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
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
            mDobTextInputLayout.setError(getString(R.string.error_invalid_dob));
        } else {
            mDobTextInputLayout.setError(null);
            String dob = getString(R.string.dob_format, dayOfMonth, month + 1, year);
            mDobEditText.setText(dob);
            mDocumentHasChanged = !dob.equals(mUsersDocument.getDob());
        }
    }
}