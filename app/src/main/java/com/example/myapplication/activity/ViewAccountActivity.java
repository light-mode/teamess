package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;
import com.example.myapplication.document.UsersDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViewAccountActivity extends BaseActivity {
    public static final String TAG = "ViewActivity";

    private ImageView mAvatarImageView;
    private TextInputEditText mUsernameEditText;
    private TextInputEditText mDobEditText;
    private TextInputEditText mBioEditText;
    private MaterialButton mChatButton;

    private String mOtherUid;
    private final ActivityResultLauncher<Intent> mActivityLauncher;

    public ViewAccountActivity() {
        mActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_account);
        setTitle(getString(R.string.activity_view_account_title));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        setReference();
        setDefault();
        loadData();
    }

    private void setReference() {
        mAvatarImageView = findViewById(R.id.activity_view_account_image_view_avatar);
        mUsernameEditText = findViewById(R.id.activity_view_account_edit_text_username);
        mDobEditText = findViewById(R.id.activity_view_account_edit_text_dob);
        mBioEditText = findViewById(R.id.activity_view_account_edit_text_bio);
        mChatButton = findViewById(R.id.activity_view_account_button_chat);
    }

    private void setDefault() {
        mUsernameEditText.setInputType(InputType.TYPE_NULL);
        mDobEditText.setInputType(InputType.TYPE_NULL);
        mBioEditText.setInputType(InputType.TYPE_NULL);
    }

    private void loadData() {
        String otherUid = getIntent().getStringExtra(Constants.EXTRA_OTHER_UID);
        if (otherUid == null) {
            throw new RuntimeException();
        }
        mOtherUid = otherUid;
        Utils.getUsersRef().document(mOtherUid).get()
                .addOnCompleteListener(this::onGetUsersDocumentComplete);
    }

    private void onGetUsersDocumentComplete(@NonNull Task<DocumentSnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) {
            Utils.logTaskException(TAG, task);
            return;
        }
        UsersDocument usersDocument = Objects.requireNonNull(task.getResult().toObject(UsersDocument.class));
        mUsernameEditText.setText(usersDocument.getUsername());
        mBioEditText.setText(usersDocument.getBio());
        mDobEditText.setText(usersDocument.getDob());
        Glide.with(this).load(Utils.getUsersAvatarRef(mOtherUid))
                .error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        mChatButton.setOnClickListener(v -> onChatButtonClick());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mChatButton.setOnClickListener(v -> onChatButtonClick());
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    private void onChatButtonClick() {
        Editable otherUsername = mUsernameEditText.getText();
        if (otherUsername == null) {
            return;
        }
        Map<String, String> othersUsername = new HashMap<>();
        othersUsername.put(mOtherUid, otherUsername.toString());
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_AUTHENTICATED, true);
        intent.putExtra(Constants.EXTRA_CHAT_TYPE, Constants.CHAT_TYPE_SINGLE);
        intent.putExtra(Constants.EXTRA_OTHER_UID, mOtherUid);
        intent.putExtra(Constants.EXTRA_OTHERS_USERNAME, (Serializable) othersUsername);
        mActivityLauncher.launch(intent);
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
        return findViewById(R.id.activity_view_account_root_layout);
    }
}