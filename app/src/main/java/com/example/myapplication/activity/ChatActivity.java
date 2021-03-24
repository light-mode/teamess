package com.example.myapplication.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.document.ChatsDocument;
import com.example.myapplication.document.ChatsMessagesDocument;
import com.example.myapplication.document.UsersChatsDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends BaseActivity implements MessageAdapter.OnClickListener {
    public static final String TAG = "ChatActivity";

    private ImageView mAvatarImageView;
    private TextView mChatNameTextView;
    private TextView mStatusTextView;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private FloatingActionButton mUploadFileButton;
    private FloatingActionButton mCameraButton;
    private FloatingActionButton mAddReactionButton;
    private FloatingActionButton mVerySatisfiedButton;
    private FloatingActionButton mSatisfiedAltButton;
    private FloatingActionButton mSatisfiedButton;
    private FloatingActionButton mNeutralButton;
    private FloatingActionButton mDissatisfiedButton;
    private FloatingActionButton mVeryDissatisfiedButton;
    private FloatingActionButton mSendButton;
    private EditText mMessageEditText;

    private final String mCurrentUid;
    private final Map<String, String> mOthersStatus;
    private final ActivityResultLauncher<Intent> mCameraLauncher;
    private final ActivityResultLauncher<Intent> mFileChooserLauncher;
    private final List<ChatsMessagesDocument> mChatsMessagesDocuments;

    public String mChatId;
    public String mChatType;
    public String mChatName;
    private String mOtherUid;
    private String mChatCreatorUid;
    private String mAvatarTimestamp;
    private List<String> mMembersUid;
    private boolean mReactionIconsVisible;
    private MessageAdapter mMessageAdapter;
    private Map<String, String> mOthersUsername;
    private ChatsMessagesDocument mDownloadPending;

    public ChatActivity() {
        mCurrentUid = Utils.getCurrentUid();
        mOthersStatus = new HashMap<>();
        mChatsMessagesDocuments = new ArrayList<>();
        mCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onRequestOpenCameraReturn);
        mFileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onRequestOpenFileChooserReturn);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setReference();
        setActionBar();
        setDefault();
        setListener();
        switch (mChatType) {
            case Constants.CHAT_TYPE_SINGLE:
                addSingleChatsDocumentListener();
                if (mChatId == null) {
                    addUsersChatsListener();
                } else {
                    addChatsMessagesListener();
                }
                break;
            case Constants.CHAT_TYPE_GROUP:
                addGroupChatsDocumentListener();
                addChatsMessagesListener();
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void setReference() {
        Intent intent = getIntent();
        mChatId = intent.getStringExtra(Constants.EXTRA_CHAT_ID);
        mChatType = intent.getStringExtra(Constants.EXTRA_CHAT_TYPE);
        mOtherUid = intent.getStringExtra(Constants.EXTRA_OTHER_UID);
        mOthersUsername = (Map<String, String>) intent.getSerializableExtra(Constants.EXTRA_OTHERS_USERNAME);

        mRecyclerView = findViewById(R.id.activity_chat_recycler_view);
        mProgressBar = findViewById(R.id.activity_chat_progress_bar);
        mUploadFileButton = findViewById(R.id.activity_chat_button_upload_file);
        mCameraButton = findViewById(R.id.activity_chat_button_camera);
        mAddReactionButton = findViewById(R.id.activity_chat_button_add_reaction);
        mVerySatisfiedButton = findViewById(R.id.activity_chat_button_very_satisfied);
        mSatisfiedAltButton = findViewById(R.id.activity_chat_button_satisfied_alt);
        mSatisfiedButton = findViewById(R.id.activity_chat_button_satisfied);
        mNeutralButton = findViewById(R.id.activity_chat_button_neutral);
        mDissatisfiedButton = findViewById(R.id.activity_chat_button_dissatisfied);
        mVeryDissatisfiedButton = findViewById(R.id.activity_chat_button_very_dissatisfied);
        mSendButton = findViewById(R.id.activity_chat_button_send);
        mMessageEditText = findViewById(R.id.activity_chat_edit_text_message);
    }

    private void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new RuntimeException();
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        View view = View.inflate(this, R.layout.activity_chat_action_bar, null);
        mAvatarImageView = view.findViewById(R.id.activity_chat_action_bar_image_view_avatar);
        StorageReference avatarRef;
        if (Constants.CHAT_TYPE_SINGLE.equals(mChatType)) {
            avatarRef = Utils.getUsersAvatarRef(mOtherUid);
        } else {
            avatarRef = Utils.getChatsAvatarRef(mChatId);
        }
        Glide.with(this).load(avatarRef).error(Constants.CHAT_TYPE_SINGLE.equals(mChatType)
                ? R.drawable.ic_baseline_person_24
                : R.drawable.ic_baseline_group_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mAvatarImageView);
        mChatNameTextView = view.findViewById(R.id.activity_chat_action_bar_text_view_chat_name);
        if (Constants.CHAT_TYPE_SINGLE.equals(mChatType)) {
            mChatNameTextView.setText(mOthersUsername.get(mOtherUid));
        } else {
            mChatNameTextView.setText(mChatName);
        }
        mStatusTextView = view.findViewById(R.id.activity_chat_action_bar_text_view_status);
        view.findViewById(R.id.activity_chat_action_bar_image_view_more)
                .setOnClickListener(this::onMoreIconClick);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(view, layoutParams);
    }

    private void setDefault() {
        mStatusTextView.setTextColor(Color.GRAY);
        mStatusTextView.setText(Constants.STATUS_OFFLINE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mMessageAdapter = new MessageAdapter(this, mChatId, mChatType, mChatsMessagesDocuments, mOthersUsername, this);
        mRecyclerView.setAdapter(mMessageAdapter);
    }

    private void setListener() {
        mUploadFileButton.setOnClickListener(v -> onUploadFileButtonClick());
        mCameraButton.setOnClickListener(v -> onCameraButtonClick());
        mAddReactionButton.setOnClickListener(v -> onAddReactionButtonClick());
        mVerySatisfiedButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_VERY_SATISFIED));
        mSatisfiedAltButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_SATISFIED_ALT));
        mSatisfiedButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_SATISFIED));
        mNeutralButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_NEUTRAL));
        mDissatisfiedButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_DISSATISFIED));
        mVeryDissatisfiedButton.setOnClickListener(v -> onReactionButtonClick(Constants.ICON_VERY_DISSATISFIED));
        mSendButton.setOnClickListener(v -> onSendButtonClick());
        mMessageEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && mReactionIconsVisible) {
                onAddReactionButtonClick();
            } else if (hasFocus) {
                mMessageEditText.requestFocus();
            }
        });
        mMessageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (EditorInfo.IME_ACTION_DONE == actionId && !mMessageEditText.getText().toString().isEmpty()) {
                onSendButtonClick();
            }
            return false;
        });
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    mSendButton.setVisibility(View.INVISIBLE);
                } else {
                    if (mReactionIconsVisible) {
                        onAddReactionButtonClick();
                    }
                    mSendButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void addUsersChatsListener() {
        Utils.getUsersChatsRef(mCurrentUid).addSnapshotListener((value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType().equals(DocumentChange.Type.ADDED)) {
                    QueryDocumentSnapshot queryDocumentSnapshot = documentChange.getDocument();
                    UsersChatsDocument usersChatsDocument = queryDocumentSnapshot.toObject(UsersChatsDocument.class);
                    boolean singleChatType = Constants.CHAT_TYPE_SINGLE.equals(usersChatsDocument.getType());
                    boolean sameOtherUser = mOtherUid.equals(usersChatsDocument.getOtherUid());
                    if (singleChatType && sameOtherUser) {
                        mChatId = queryDocumentSnapshot.getId();
                        addChatsMessagesListener();
                    }
                }
            }
        });
    }

    private void addSingleChatsDocumentListener() {
        Utils.getUsersRef().document(mOtherUid).addSnapshotListener(this, (value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            String status = Objects.requireNonNull(value.get(Constants.FIELD_STATUS)).toString();
            mStatusTextView.setText(status);
            mStatusTextView.setTextColor(Constants.STATUS_OFFLINE.equals(status) ? Color.GRAY : Color.GREEN);
        });
    }

    private void onRequestOpenCameraReturn(@NonNull ActivityResult result) {
        Intent intent = result.getData();
        if (result.getResultCode() != RESULT_OK || intent == null || intent.getExtras() == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        Bitmap bitmap = (Bitmap) intent.getExtras().get("data");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Timestamp now = Timestamp.now();
        String photoId = now.getSeconds() + Long.toString(now.getNanoseconds()) + ".jpeg";
        Utils.getChatsFileRef(mChatId, photoId).putBytes(data).addOnCompleteListener(uploadPhotoTask -> {
            if (!uploadPhotoTask.isSuccessful() || uploadPhotoTask.getResult() == null) {
                Utils.logTaskException(TAG, uploadPhotoTask);
                return;
            }
            uploadPhotoTask.getResult().getStorage().getDownloadUrl().addOnCompleteListener(getDownloadUrlTask -> {
                if (!getDownloadUrlTask.isSuccessful() || getDownloadUrlTask.getResult() == null) {
                    Utils.logTaskException(TAG, getDownloadUrlTask);
                    return;
                }
                String downloadUrl = getDownloadUrlTask.getResult().toString();
                checkAddChatsMessagesDocument(photoId, Constants.MESSAGE_TYPE_IMAGE, downloadUrl);
            });
        });
    }

    private void onRequestOpenFileChooserReturn(@NonNull ActivityResult result) {
        Intent intent = result.getData();
        if (result.getResultCode() == RESULT_CANCELED || intent == null || intent.getData() == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        Uri uri = intent.getData();
        Timestamp now = Timestamp.now();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, getString(R.string.error_upload_file_unknown), Toast.LENGTH_LONG).show();
            return;
        }
        int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        String fileName = cursor.getString(displayNameIndex);
        long size = cursor.getLong(sizeIndex);
        cursor.close();
        if (size > Constants.FILE_UPLOAD_LIMIT_IN_BYTE) {
            mProgressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.error_upload_file_limit), Toast.LENGTH_LONG).show();
            return;
        }
        String extension = fileName.split("\\.")[fileName.split("\\.").length - 1];
        String fileId = now.getSeconds() + Long.toString(now.getNanoseconds()) + "." + extension;
        Utils.getChatsFileRef(mChatId, fileId).putFile(uri).addOnCompleteListener(uploadFileTask -> {
            if (!uploadFileTask.isSuccessful() || uploadFileTask.getResult() == null) {
                Utils.logTaskException(TAG, uploadFileTask);
                return;
            }
            uploadFileTask.getResult().getStorage().getDownloadUrl().addOnCompleteListener(getDownloadUrlTask -> {
                if (!getDownloadUrlTask.isSuccessful() || getDownloadUrlTask.getResult() == null) {
                    Utils.logTaskException(TAG, getDownloadUrlTask);
                    return;
                }
                String downloadUrl = getDownloadUrlTask.getResult().toString();
                if (getContentResolver().getType(uri).contains("image/")) {
                    checkAddChatsMessagesDocument(fileId, Constants.MESSAGE_TYPE_IMAGE, downloadUrl);
                } else {
                    checkAddChatsMessagesDocument(fileName, Constants.MESSAGE_TYPE_FILE, downloadUrl);
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void addGroupChatsDocumentListener() {
        Utils.getChatsRef().document(mChatId).addSnapshotListener((value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            if (mChatCreatorUid == null) {
                mChatCreatorUid = Objects.requireNonNull(value.get(Constants.FIELD_CREATOR_UID)).toString();
            }
            String chatName = Objects.requireNonNull(value.get(Constants.FIELD_NAME)).toString();
            if (!chatName.equals(mChatName)) {
                mChatName = chatName;
                mChatNameTextView.setText(mChatName);
            }
            String avatarTimestamp = Objects.requireNonNull(value.get(Constants.FIELD_AVATAR_TIMESTAMP)).toString();
            if (!avatarTimestamp.equals(mAvatarTimestamp)) {
                mAvatarTimestamp = avatarTimestamp;
                Glide.with(this).load(Utils.getChatsAvatarRef(mChatId))
                        .error(R.drawable.ic_baseline_group_24)
                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(mAvatarImageView);
            }
            List<String> membersUid = Objects.requireNonNull((ArrayList<String>) value.get(Constants.FIELD_MEMBERS_UID));
            if (!membersUid.contains(mCurrentUid)) {
                finish();
            }
            if (mMembersUid == null || mMembersUid.size() < membersUid.size()) {
                mMembersUid = membersUid;
                addUsersStatusListener();
            } else if (mMembersUid.size() > membersUid.size()) {
                mMembersUid.removeAll(membersUid);
                String removedUid = membersUid.get(0);
                if (removedUid.equals(mCurrentUid)) {
                    finish();
                } else {
                    mMembersUid = membersUid;
                    addUsersStatusListener();
                }
            }
        });
    }

    private void addUsersStatusListener() {
        mOthersStatus.clear();
        for (String uid : mMembersUid) {
            if (uid.equals(mCurrentUid)) {
                continue;
            }
            Utils.getUsersRef().document(uid).addSnapshotListener((value, error) -> {
                if (value == null) {
                    Log.e(TAG, error == null ? "" : error.getMessage());
                    return;
                }
                String status = Objects.requireNonNull(value.get(Constants.FIELD_STATUS)).toString();
                mOthersStatus.put(uid, status);
                if (mOthersStatus.containsValue(Constants.STATUS_ONLINE)) {
                    mStatusTextView.setTextColor(Color.GREEN);
                    mStatusTextView.setText(Constants.STATUS_ONLINE);
                } else {
                    mStatusTextView.setTextColor(Color.GRAY);
                    mStatusTextView.setText(Constants.STATUS_OFFLINE);
                }
            });
        }
    }

    private void addChatsMessagesListener() {
        Utils.getChatsMessagesRef(mChatId).addSnapshotListener((value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                int newIndex = documentChange.getNewIndex();
                ChatsMessagesDocument chatsMessagesDocument = documentChange.getDocument().toObject(ChatsMessagesDocument.class);
                switch (documentChange.getType()) {
                    case ADDED:
                        mChatsMessagesDocuments.add(chatsMessagesDocument);
                        mMessageAdapter.notifyItemInserted(newIndex);
                        mRecyclerView.smoothScrollToPosition(newIndex);
                        if (Constants.MESSAGE_TYPE_SYSTEM.equals(chatsMessagesDocument.getType())
                                || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            break;
                        }
                        List<String> seenUserIds = chatsMessagesDocument.getSeenUserIds();
                        if (seenUserIds.contains(mCurrentUid)) {
                            break;
                        }
                        seenUserIds.add(mCurrentUid);
                        Utils.getChatsMessagesRef(mChatId).document(chatsMessagesDocument.getId())
                                .update(Constants.COLLECTION_SEEN_USER_IDS, seenUserIds);
                        break;
                    case MODIFIED:
                        mChatsMessagesDocuments.set(newIndex, chatsMessagesDocument);
                        mMessageAdapter.notifyItemChanged(newIndex);
                        break;
                    case REMOVED:
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (int index = mChatsMessagesDocuments.size() - 1; index >= 0; index--) {
            ChatsMessagesDocument chatsMessagesDocument = mChatsMessagesDocuments.get(index);
            if (Constants.MESSAGE_TYPE_SYSTEM.equals(chatsMessagesDocument.getType())) {
                continue;
            }
            List<String> seenUserIds = chatsMessagesDocument.getSeenUserIds();
            if (seenUserIds.contains(mCurrentUid)) {
                return;
            } else {
                seenUserIds.add(mCurrentUid);
                Utils.getChatsMessagesRef(mChatId).document(chatsMessagesDocument.getId())
                        .update(Constants.COLLECTION_SEEN_USER_IDS, seenUserIds);
            }
        }
    }

    private void onUploadFileButtonClick() {
        if (mMessageEditText.hasFocus()) {
            mMessageEditText.clearFocus();
        }
        if (mReactionIconsVisible) {
            onAddReactionButtonClick();
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        mFileChooserLauncher.launch(intent);
    }

    private void onCameraButtonClick() {
        if (mMessageEditText.hasFocus()) {
            mMessageEditText.clearFocus();
        }
        if (mReactionIconsVisible) {
            onAddReactionButtonClick();
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mCameraLauncher.launch(cameraIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Constants.REQUEST_CAMERA_PERMISSION_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onCameraButtonClick();
            }
        } else if (Constants.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                download(mDownloadPending);
            }
        }
    }

    private void onAddReactionButtonClick() {
        mReactionIconsVisible = !mReactionIconsVisible;
        mAddReactionButton.setImageResource(mReactionIconsVisible ? R.drawable.ic_baseline_cancel_24
                : R.drawable.ic_baseline_add_reaction_24);
        changeReactionIconsVisibility(mReactionIconsVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void changeReactionIconsVisibility(int visibility) {
        mVerySatisfiedButton.setVisibility(visibility);
        mSatisfiedAltButton.setVisibility(visibility);
        mSatisfiedButton.setVisibility(visibility);
        mNeutralButton.setVisibility(visibility);
        mDissatisfiedButton.setVisibility(visibility);
        mVeryDissatisfiedButton.setVisibility(visibility);
    }

    private void onReactionButtonClick(int icon) {
        checkAddChatsMessagesDocument(icon + "", Constants.MESSAGE_TYPE_ICON, null);
    }

    private void onSendButtonClick() {
        String content = mMessageEditText.getText().toString();
        if (content.isEmpty()) {
            return;
        }
        checkAddChatsMessagesDocument(content, Constants.MESSAGE_TYPE_TEXT, null);
    }

    private void checkAddChatsMessagesDocument(String content, String type, String downloadUrl) {
        if (mChatId == null) {
            addChatsDocument(content, type, downloadUrl);
        } else {
            addChatsMessagesDocument(content, type, downloadUrl);
        }
    }

    private void addChatsDocument(String content, String type, String downloadUrl) {
        ChatsDocument chatsDocument = new ChatsDocument();
        chatsDocument.setType(Constants.CHAT_TYPE_SINGLE);
        Utils.getChatsRef().add(chatsDocument).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Utils.logTaskException(TAG, task);
                return;
            }
            mChatId = task.getResult().getId();
            addChatsMessagesDocument(content, type, downloadUrl);
        });
    }

    private void addChatsMessagesDocument(String content, String type, String downloadUrl) {
        Timestamp now = Timestamp.now();
        String id = now.toString();
        String timestamp = Long.toString(now.getSeconds());
        ChatsMessagesDocument chatsMessagesDocument = new ChatsMessagesDocument();
        chatsMessagesDocument.setId(id);
        chatsMessagesDocument.setType(type);
        chatsMessagesDocument.setContent(content);
        chatsMessagesDocument.setTimestamp(timestamp);
        chatsMessagesDocument.setSenderUid(mCurrentUid);
        chatsMessagesDocument.setDownloadUrl(downloadUrl);
        chatsMessagesDocument.setSeenUserIds(Collections.singletonList(mCurrentUid));
        Utils.getChatsMessagesRef(mChatId).document(id).set(chatsMessagesDocument).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            addUsersChatsDocument(type, content, timestamp);
        });
    }

    private void addUsersChatsDocument(String type, String content, String timestamp) {
        UsersChatsDocument usersChatsDocument = new UsersChatsDocument();
        usersChatsDocument.setId(mChatId);
        usersChatsDocument.setType(mChatType);
        usersChatsDocument.setOtherUid(mOtherUid);
        usersChatsDocument.setLastMessageType(type);
        usersChatsDocument.setLastMessageContent(content);
        usersChatsDocument.setLastMessageTimestamp(timestamp);
        usersChatsDocument.setLastMessageSenderUid(mCurrentUid);
        usersChatsDocument.setLastMessageDeleted(false);
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        switch (mChatType) {
            case Constants.CHAT_TYPE_SINGLE:
                usersChatsDocument.setOtherUid(mOtherUid);
                batch.set(Utils.getUsersChatsRef(mCurrentUid).document(mChatId), usersChatsDocument);
                usersChatsDocument.setOtherUid(mCurrentUid);
                batch.set(Utils.getUsersChatsRef(mOtherUid).document(mChatId), usersChatsDocument);
                break;
            case Constants.CHAT_TYPE_GROUP:
                for (String uid : mMembersUid) {
                    batch.set(Utils.getUsersChatsRef(uid).document(mChatId), usersChatsDocument);
                }
                break;
            default:
                throw new RuntimeException();
        }
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            setupForNewMessage(type);
        });
    }

    private void setupForNewMessage(String type) {
        switch (type) {
            case Constants.MESSAGE_TYPE_TEXT:
                mMessageEditText.setText("");
                break;
            case Constants.MESSAGE_TYPE_ICON:
                onAddReactionButtonClick();
                break;
            case Constants.MESSAGE_TYPE_IMAGE:
            case Constants.MESSAGE_TYPE_FILE:
                mProgressBar.setVisibility(View.INVISIBLE);
                break;
            default:
                throw new RuntimeException();
        }
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
    public void onImageContentClick(String imageId, Drawable image) {
        Bitmap bitmap = ((BitmapDrawable) image).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bytes = stream.toByteArray();
        Intent intent = new Intent(this, ViewImageActivity.class);
        intent.putExtra(Constants.EXTRA_IMAGE, bytes);
        startActivity(intent);
    }

    @Override
    public void onDownloadButtonClick(ChatsMessagesDocument chatsMessagesDocument) {
        mDownloadPending = chatsMessagesDocument;
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
        if (PackageManager.PERMISSION_DENIED == permissionStatus) {
            String[] permissions = new String[]{permission};
            int requestCode = Constants.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE;
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        } else {
            download(chatsMessagesDocument);
        }
    }

    @Override
    public void onDeleteButtonClick(@NonNull ChatsMessagesDocument chatsMessagesDocument) {
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        String messageId = chatsMessagesDocument.getId();
        batch.update(Utils.getChatsMessagesRef(mChatId).document(messageId), Constants.FIELD_DELETED, true);
        String lastMessageId = mChatsMessagesDocuments.get(mChatsMessagesDocuments.size() - 1).getId();
        if (!messageId.equals(lastMessageId)) {
            batch.commit();
            return;
        }
        switch (mChatType) {
            case Constants.CHAT_TYPE_SINGLE:
                batch.update(Utils.getUsersChatsRef(mCurrentUid).document(mChatId), Constants.FIELD_LAST_MESSAGE_DELETED, true);
                batch.update(Utils.getUsersChatsRef(mOtherUid).document(mChatId), Constants.FIELD_LAST_MESSAGE_DELETED, true);
                break;
            case Constants.CHAT_TYPE_GROUP:
                for (String uid : mMembersUid) {
                    batch.update(Utils.getUsersChatsRef(uid).document(mChatId), Constants.FIELD_LAST_MESSAGE_DELETED, true);
                }
                break;
            default:
                throw new RuntimeException();
        }
        batch.commit();
    }

    private void download(ChatsMessagesDocument chatsMessagesDocument) {
        if (chatsMessagesDocument == null) {
            return;
        }
        DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(chatsMessagesDocument.getDownloadUrl());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, chatsMessagesDocument.getContent());
        downloadmanager.enqueue(request);
    }

    private void onMoreIconClick(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        if (Constants.CHAT_TYPE_GROUP.equals(mChatType) && mCurrentUid.equals(mChatCreatorUid)) {
            popupMenu.getMenuInflater().inflate(R.menu.menu_chat_group_creator, popupMenu.getMenu());
        } else if (Constants.CHAT_TYPE_GROUP.equals(mChatType)) {
            popupMenu.getMenuInflater().inflate(R.menu.menu_chat_group, popupMenu.getMenu());
        } else {
            popupMenu.getMenuInflater().inflate(R.menu.menu_chat_single, popupMenu.getMenu());
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (R.id.menu_chat_item_info == itemId) {
                onMenuItemClick(mCurrentUid.equals(mChatCreatorUid)
                        ? Constants.MODE_EDIT_GROUP
                        : Constants.MODE_VIEW_GROUP);
                return true;
            } else if (R.id.menu_chat_item_add_member == itemId) {
                onMenuItemClick(Constants.MODE_ADD_MEMBER);
                return true;
            } else if (R.id.menu_chat_item_remove_member == itemId) {
                onMenuItemClick(Constants.MODE_REMOVE_MEMBER);
                return true;
            } else if (R.id.menu_chat_item_leave == itemId) {
                onLeaveMenuItemClick();
                return true;
            } else if (R.id.menu_chat_item_block == itemId) {
                onBlockMenuItemClick();
                return true;
            } else {
                return false;
            }
        });
        popupMenu.show();
    }

    private void onMenuItemClick(String mode) {
        Intent intent = new Intent(this, GroupActivity.class);
        intent.putExtra(Constants.EXTRA_MODE, mode);
        intent.putExtra(Constants.EXTRA_CHAT_ID, mChatId);
        intent.putExtra(Constants.EXTRA_OTHERS_USERNAME, (Serializable) mOthersUsername);
        startActivity(intent);
    }

    private void onLeaveMenuItemClick() {
        AlertDialog.Builder leaveDialog = new AlertDialog.Builder(this);
        leaveDialog.setTitle(getString(R.string.dialog_leave_group_title, mChatName));
        leaveDialog.setMessage(getString(R.string.dialog_leave_group_message));
        leaveDialog.setPositiveButton(getString(R.string.dialog_leave_group_positive_button_text), (dialog, which) -> {
            mMembersUid.remove(mCurrentUid);
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            batch.update(Utils.getChatsRef().document(mChatId), Constants.FIELD_MEMBERS_UID, mMembersUid);
            batch.delete(Utils.getUsersChatsRef(mCurrentUid).document(mChatId));
            batch.commit().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Utils.logTaskException(TAG, task);
                    return;
                }
                String content = mCurrentUid + Constants.USER_REMOVED + mCurrentUid;
                Utils.addSystemChatsMessagesDocument(TAG, mChatId, content, mMembersUid, this::finish);
            });
        });
        leaveDialog.setNegativeButton(getString(R.string.dialog_leave_group_negative_button_text), null);
        leaveDialog.show();
    }

    private void onBlockMenuItemClick() {
        String username = mOthersUsername.values().toArray()[0].toString();
        AlertDialog.Builder blockDialog = new AlertDialog.Builder(this);
        blockDialog.setTitle(getString(R.string.dialog_block_title, username));
        blockDialog.setMessage(getString(R.string.dialog_block_message, username));
        blockDialog.setPositiveButton(getString(R.string.dialog_block_positive_button_text), (dialog, which) -> {
            // TODO
        });
        blockDialog.setNegativeButton(getString(R.string.dialog_block_negative_button_text), null);
        blockDialog.show();
    }

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_chat_root_layout);
    }
}