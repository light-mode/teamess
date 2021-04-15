package com.example.myapplication.fragment;

import android.Manifest;
import android.content.Context;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.myapplication.R;
import com.example.myapplication.activity.ChatActivity;
import com.example.myapplication.adapter.MemberAdapter;
import com.example.myapplication.document.ChatsDocument;
import com.example.myapplication.pojo.Member;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Converter;
import com.example.myapplication.utilities.Formatter;
import com.example.myapplication.utilities.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class GroupFragment extends Fragment {
    public static final String TAG = "GroupFragment";

    private ImageView mAvatarImageView;
    private FloatingActionButton mTakePhotoButton;
    private FloatingActionButton mChooseImageButton;
    private TextInputLayout mGroupNameTextInputLayout;
    private TextInputEditText mGroupNameEditText;
    private TextInputLayout mMembersTextInputLayout;
    private TextInputEditText mMembersEditText;
    private RecyclerView mRecyclerView;
    private MaterialButton mSaveButton;
    private ProgressBar mProgressBar;

    private String mMode;
    private String mChatId;
    private String mChatName;
    private Context mContext;
    private Member mChangeMember;
    private String mChatCreatorUid;
    private MemberAdapter mAdapter;
    private String mAvatarTimestamp;
    private boolean mAvatarHasChanged;
    private FragmentActivity mActivity;
    private Map<String, String> mOthersUsername;

    private final String mCurrentUid;
    private final List<Member> mMembers;
    private final ActivityResultLauncher<Intent> mCameraLauncher;
    private final ActivityResultLauncher<Intent> mActivityLauncher;
    private final ActivityResultLauncher<Intent> mGetContentLauncher;
    private final ActivityResultLauncher<String[]> mRequestPermissionLauncher;
    private final ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            boolean removeSelf = mMembers.get(viewHolder.getAdapterPosition()).getUid().equals(mCurrentUid);
            boolean removeExistedMemberOnAddMode = Constants.MODE_ADD_MEMBER.equals(mMode)
                    && !((MemberAdapter.ViewHolder) viewHolder).mMember.equals(mChangeMember);
            if (removeSelf || removeExistedMemberOnAddMode) {
                return 0;
            }
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (Constants.MODE_CREATE_GROUP.equals(mMode)) {
                mMembers.remove(position);
                mAdapter.notifyItemRemoved(position);
                mAdapter.notifyItemRangeChanged(position, mMembers.size());
                mMembersTextInputLayout.setError(mMembers.size() == 1
                        ? getString(R.string.activity_group_text_input_layout_members_suggest)
                        : null);
            } else if (Constants.MODE_ADD_MEMBER.equals(mMode)) {
                mChangeMember = null;
                mMembers.remove(position);
                mMembersTextInputLayout.setError(null);
                mAdapter.notifyItemRemoved(position);
            } else if (Constants.MODE_REMOVE_MEMBER.equals(mMode) && mChangeMember == null) {
                mChangeMember = mMembers.get(position);
                mMembersTextInputLayout.setError(getString(R.string.fragment_group_remove_member_message, mChangeMember.getUsername()));
                mMembers.remove(mChangeMember);
                mAdapter.notifyItemRemoved(position);
                mAdapter.notifyItemRangeChanged(position, mMembers.size());
            } else if (Constants.MODE_REMOVE_MEMBER.equals(mMode)) {
                mMembers.add(mChangeMember);
                mAdapter.notifyItemInserted(position + 1);
                mChangeMember = mMembers.get(position);
                mMembersTextInputLayout.setError(getString(R.string.fragment_group_remove_member_message, mChangeMember.getUsername()));
                mMembers.remove(mChangeMember);
                mAdapter.notifyItemRemoved(position);
                mAdapter.notifyItemRangeChanged(position, mMembers.size());
            }
            checkEnableSaveButton();
        }
    });

    public GroupFragment() {
        mMembers = new ArrayList<>();
        mCurrentUid = Utils.getCurrentUid();
        mCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onRequestOpenCameraReturn);
        mGetContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onGetImageLauncherReturn);
        mRequestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionReturn);
        mActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        mContext = getContext();
        if (mActivity == null || mContext == null) {
            throw new RuntimeException();
        }
        // attachToRoot set to false is required
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setReference(view);
        setDefault();
        setListener();
    }

    private void setReference(@NonNull View view) {
        mAvatarImageView = view.findViewById(R.id.fragment_group_image_view_avatar);
        mTakePhotoButton = view.findViewById(R.id.fragment_group_button_take_photo);
        mChooseImageButton = view.findViewById(R.id.fragment_group_button_choose_image);
        mGroupNameTextInputLayout = view.findViewById(R.id.fragment_group_text_input_layout_group_name);
        mGroupNameEditText = view.findViewById(R.id.fragment_group_edit_text_group_name);
        mMembersTextInputLayout = view.findViewById(R.id.fragment_group_text_input_layout_members);
        mMembersEditText = view.findViewById(R.id.fragment_group_edit_text_members);
        mRecyclerView = view.findViewById(R.id.fragment_group_recycler_view);
        mSaveButton = view.findViewById(R.id.fragment_group_button_save);
        mProgressBar = view.findViewById(R.id.fragment_group_progress_bar);
    }

    @SuppressWarnings("unchecked")
    private void setDefault() {
        mMembersEditText.setInputType(InputType.TYPE_NULL);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter = new MemberAdapter(mContext, mMembers);
        mRecyclerView.setAdapter(mAdapter);
        mSaveButton.setEnabled(false);
        Bundle args = getArguments();
        if (args == null) {
            throw new RuntimeException();
        }
        mMode = args.getString(Constants.KEY_MODE, Constants.MODE_CREATE_GROUP);
        mChatId = args.getString(Constants.KEY_CHAT_ID);
        mOthersUsername = (Map<String, String>) args.getSerializable(Constants.KEY_OTHERS_USERNAME);
        switch (mMode) {
            case Constants.MODE_CREATE_GROUP:
                setDefaultModeCreateGroup();
                break;
            case Constants.MODE_VIEW_GROUP:
                setDefaultModeViewGroup();
                break;
            case Constants.MODE_EDIT_GROUP:
                setDefaultModeEditGroup();
                break;
            case Constants.MODE_ADD_MEMBER:
                setDefaultModeAddMember();
                break;
            case Constants.MODE_REMOVE_MEMBER:
                setDefaultModeRemoveMember();
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void setDefaultModeCreateGroup() {
        Glide.with(this).asDrawable().load(Utils.getUsersAvatarRef(mCurrentUid))
                .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Member member = new Member(mCurrentUid, "You", resource);
                        mMembers.add(member);
                        mAdapter.notifyItemInserted(mMembers.size());
                        mActivity.setTitle(getString(R.string.activity_group_title_create));
                        mAvatarImageView.setOnClickListener(v -> onAvatarImageViewClick());
                        mTakePhotoButton.setOnClickListener(v -> onTakePhotoButtonClick());
                        mChooseImageButton.setOnClickListener(v -> onChooseImageButtonClick());
                        mGroupNameTextInputLayout.setError(getString(R.string.activity_group_text_input_layout_group_name_error_lack));
                        mMembersTextInputLayout.setError(getString(R.string.activity_group_text_input_layout_members_suggest));
                        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void setDefaultModeViewGroup() {
        mActivity.setTitle(getString(R.string.activity_group_title_view));
        mSaveButton.setVisibility(View.INVISIBLE);
        disableGroupNameEditText();
        addChatsDocumentListener();
    }

    private void setDefaultModeEditGroup() {
        mActivity.setTitle(getString(R.string.activity_group_title_edit));
        mAvatarImageView.setOnClickListener(v -> onAvatarImageViewClick());
        mTakePhotoButton.setOnClickListener(v -> onTakePhotoButtonClick());
        mChooseImageButton.setOnClickListener(v -> onChooseImageButtonClick());
        addChatsDocumentListener();
    }

    private void setDefaultModeAddMember() {
        mActivity.setTitle(getString(R.string.activity_group_title_add_member));
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        disableGroupNameEditText();
        addChatsDocumentListener();
    }

    private void setDefaultModeRemoveMember() {
        mActivity.setTitle(getString(R.string.activity_group_title_remove_member));
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        disableGroupNameEditText();
        addChatsDocumentListener();
    }

    private void disableGroupNameEditText() {
        mGroupNameEditText.setFocusable(false);
        mGroupNameEditText.setFocusableInTouchMode(false);
        mGroupNameEditText.setInputType(InputType.TYPE_NULL);
        mGroupNameEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private void addChatsDocumentListener() {
        Utils.getChatsRef().document(mChatId).addSnapshotListener((value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            mChatCreatorUid = Objects.requireNonNull(value.get(Constants.FIELD_CREATOR_UID)).toString();
            String chatName = Objects.requireNonNull(value.get(Constants.FIELD_NAME)).toString();
            if (!chatName.equals(mChatName)) {
                mChatName = chatName;
                mGroupNameEditText.setText(mChatName);
            }
            String avatarTimestamp = Objects.requireNonNull(value.get(Constants.FIELD_AVATAR_TIMESTAMP)).toString();
            if (!avatarTimestamp.equals(mAvatarTimestamp)) {
                mAvatarTimestamp = avatarTimestamp;
                Glide.with(mActivity.getApplicationContext()).load(Utils.getChatsAvatarRef(mChatId))
                        .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_GROUP_AVATAR_CODE))
                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(mAvatarImageView);
            }
            List<String> membersUid = Objects.requireNonNull((ArrayList<String>) value.get(Constants.FIELD_MEMBERS_UID));
            if (!membersUid.contains(mCurrentUid)) {
                mActivity.finish();
            }
            if (mMembers.size() < membersUid.size()) {
                for (String uid : membersUid) {
                    Glide.with(mActivity.getApplicationContext()).asDrawable().load(Utils.getUsersAvatarRef(uid))
                            .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE))
                            .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    if (getContext() == null) {
                                        return;
                                    }
                                    String username = uid.equals(mCurrentUid) ? getString(R.string.you_capital) : mOthersUsername.get(uid);
                                    Member member = new Member(uid, username, resource);
                                    mMembers.add(member);
                                    mAdapter.notifyItemInserted(mMembers.size());
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }
            } else if (mMembers.size() > membersUid.size()) {
                for (int index = 0; index < mMembers.size(); index++) {
                    Member member = mMembers.get(index);
                    if (!membersUid.contains(member.getUid())) {
                        mMembers.remove(member);
                        mAdapter.notifyItemRemoved(index);
                        mAdapter.notifyItemRangeChanged(index, mMembers.size());
                        break;
                    }
                }
            }
        });
    }

    private void setListener() {
        mSaveButton.setOnClickListener(v -> onSaveButtonClick());
        mGroupNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String groupName = Formatter.formatName(s.toString());
                if (groupName.length() < Constants.NAME_LENGTH_MIN) {
                    mGroupNameTextInputLayout.setError(getString(R.string.activity_group_text_input_layout_group_name_error_min, 5));
                    mSaveButton.setEnabled(false);
                } else if (groupName.length() > Constants.NAME_LENGTH_MAX) {
                    mGroupNameTextInputLayout.setError(getString(R.string.activity_group_text_input_layout_group_name_error_max));
                    mSaveButton.setEnabled(false);
                } else if (groupName.contains("_")) {
                    mGroupNameTextInputLayout.setError(getString(R.string.activity_group_text_input_layout_group_name_error_contain_underscore));
                    mSaveButton.setEnabled(false);
                } else {
                    mGroupNameTextInputLayout.setError(null);
                    checkEnableSaveButton();
                }
            }
        });
    }

    private void onAvatarImageViewClick() {
        if (mChooseImageButton.getVisibility() == View.INVISIBLE && mTakePhotoButton.getVisibility() == View.INVISIBLE) {
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
        if (mContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mRequestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        } else {
            mChooseImageButton.setVisibility(View.INVISIBLE);
            mTakePhotoButton.setVisibility(View.INVISIBLE);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mCameraLauncher.launch(cameraIntent);
        }
    }

    private void onRequestPermissionReturn(@NonNull Map<String, Boolean> result) {
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (entry.getKey().equals(Manifest.permission.CAMERA) && entry.getValue().equals(true)) {
                mChooseImageButton.setVisibility(View.INVISIBLE);
                mTakePhotoButton.setVisibility(View.INVISIBLE);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                mCameraLauncher.launch(cameraIntent);
                break;
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
        Glide.with(this).load(data)
                .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mAvatarHasChanged = true;
                        checkEnableSaveButton();
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    private void onGetImageLauncherReturn(@NonNull ActivityResult result) {
        if (result.getResultCode() == RESULT_CANCELED || result.getData() == null) {
            return;
        }
        Uri uri = result.getData().getData();
        Glide.with(this).load(uri)
                .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_GROUP_AVATAR_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mAvatarHasChanged = true;
                        checkEnableSaveButton();
                        return false;
                    }
                }).into(mAvatarImageView);
    }

    public void onSearchResultSelect(String uid, String username, Drawable avatar) {
        for (Member member : mMembers) {
            if (member.getUid().equals(uid)) {
                Toast.makeText(mContext, getString(R.string.error_select_existed_member), Toast.LENGTH_LONG).show();
                return;
            }
        }
        Member member = new Member(uid, username, avatar);
        if (Constants.MODE_CREATE_GROUP.equals(mMode)) {
            mMembers.add(member);
            mAdapter.notifyItemInserted(mMembers.size());
            mMembersTextInputLayout.setError(null);
        } else if (Constants.MODE_ADD_MEMBER.equals(mMode) && mChangeMember == null) {
            mChangeMember = member;
            mMembersTextInputLayout.setError(getString(R.string.fragment_group_add_member_message, mChangeMember.getUsername()));
            mMembers.add(member);
            mAdapter.notifyItemInserted(mMembers.size());
        } else if (Constants.MODE_ADD_MEMBER.equals(mMode)) {
            mMembers.remove(mChangeMember);
            mAdapter.notifyItemRemoved(mMembers.size());
            mChangeMember = member;
            mMembersTextInputLayout.setError(getString(R.string.fragment_group_add_member_message, mChangeMember.getUsername()));
            mMembers.add(member);
            mAdapter.notifyItemInserted(mMembers.size());
        }
        checkEnableSaveButton();
    }

    private void checkEnableSaveButton() {
        boolean groupNameValid = mGroupNameTextInputLayout.getError() == null;
        switch (mMode) {
            case Constants.MODE_CREATE_GROUP:
                boolean membersValid = mMembersTextInputLayout.getError() == null;
                mSaveButton.setEnabled(groupNameValid && membersValid);
                break;
            case Constants.MODE_ADD_MEMBER:
            case Constants.MODE_REMOVE_MEMBER:
                mSaveButton.setEnabled(mChangeMember != null);
                break;
            case Constants.MODE_EDIT_GROUP:
                String groupName = Formatter.formatName(mGroupNameEditText.getEditableText().toString());
                boolean groupNameHasChanged = !mChatName.equals(groupName);
                mSaveButton.setEnabled(groupNameValid && (mAvatarHasChanged || groupNameHasChanged));
                break;
            default:
                break;
        }
    }

    private void onSaveButtonClick() {
        mSaveButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        switch (mMode) {
            case Constants.MODE_CREATE_GROUP:
                addChatsDocument();
                break;
            case Constants.MODE_ADD_MEMBER:
                addMemberAddedMessage();
                break;
            case Constants.MODE_REMOVE_MEMBER:
                addMemberRemovedMessage();
                break;
            case Constants.MODE_EDIT_GROUP:
                uploadGroupAvatar();
                break;
            default:
                break;
        }
    }

    private void addChatsDocument() {
        mChatCreatorUid = mCurrentUid;
        mChatName = Formatter.formatName(mGroupNameEditText.getEditableText().toString());
        List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
        ChatsDocument chatsDocument = new ChatsDocument();
        chatsDocument.setName(mChatName);
        chatsDocument.setMembersUid(membersUid);
        chatsDocument.setCreatorUid(mChatCreatorUid);
        chatsDocument.setType(Constants.CHAT_TYPE_GROUP);
        chatsDocument.setAvatarTimestamp(Timestamp.now().toString());
        Utils.getChatsRef().add(chatsDocument).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Utils.logTaskException(TAG, task);
                return;
            }
            mChatId = task.getResult().getId();
            uploadGroupAvatarOnCreateGroup();
        });
    }

    private void uploadGroupAvatarOnCreateGroup() {
        Drawable drawable = mAvatarImageView.getDrawable();
        Bitmap bitmap;
        if (mAvatarHasChanged) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Converter.convertDrawableToBitmap(drawable);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Utils.getChatsAvatarRef(mChatId).putBytes(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            addGroupCreatedMessage();
        });
    }

    private void addGroupCreatedMessage() {
        String content = mCurrentUid + Constants.GROUP_CREATED + mChatName;
        List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
        Utils.addSystemChatsMessagesDocument(TAG, mChatId, Constants.CHAT_TYPE_GROUP, content, membersUid, () -> {
            Map<String, String> othersUsername = new HashMap<>();
            for (Member member : mMembers) {
                othersUsername.put(member.getUid(), member.getUsername());
            }
            Intent intent = new Intent(mActivity, ChatActivity.class);
            intent.putExtra(Constants.EXTRA_AUTHENTICATED, true);
            intent.putExtra(Constants.EXTRA_CHAT_ID, mChatId);
            intent.putExtra(Constants.EXTRA_CHAT_TYPE, Constants.CHAT_TYPE_GROUP);
            intent.putExtra(Constants.EXTRA_OTHERS_USERNAME, (Serializable) othersUsername);
            mActivityLauncher.launch(intent);
            mActivity.finish();
        });
    }

    private void uploadGroupAvatar() {
        if (!mAvatarHasChanged) {
            updateChatsDocumentName();
            return;
        }
        Drawable drawable = mAvatarImageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        Utils.getChatsAvatarRef(mChatId).putBytes(data).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            addGroupAvatarChangedMessage();
        });
    }

    private void addMemberAddedMessage() {
        List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
        Utils.getChatsRef().document(mChatId).update(Constants.FIELD_MEMBERS_UID, membersUid).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            String content = mCurrentUid + Constants.USER_ADDED + mChangeMember.getUid();
            Utils.addSystemChatsMessagesDocument(TAG, mChatId, Constants.CHAT_TYPE_GROUP, content, membersUid, () -> mActivity.finish());
        });
    }

    private void addMemberRemovedMessage() {
        List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        batch.update(Utils.getChatsRef().document(mChatId), Constants.FIELD_MEMBERS_UID, membersUid);
        batch.delete(Utils.getUsersChatsRef(mChangeMember.getUid()).document(mChatId));
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            String content = mCurrentUid + Constants.USER_REMOVED + mChangeMember.getUid();
            Utils.addSystemChatsMessagesDocument(TAG, mChatId, Constants.CHAT_TYPE_GROUP, content, membersUid, () -> mActivity.finish());
        });
    }

    private void addGroupAvatarChangedMessage() {
        String content = mCurrentUid + Constants.GROUP_AVATAR_CHANGED;
        List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
        Utils.addSystemChatsMessagesDocument(TAG, mChatId, Constants.CHAT_TYPE_GROUP, content, membersUid, this::updateChatsDocumentAvatarTimestamp);
    }

    private void updateChatsDocumentAvatarTimestamp() {
        String timestamp = Timestamp.now().toString();
        Utils.getChatsRef().document(mChatId)
                .update(Constants.FIELD_AVATAR_TIMESTAMP, timestamp).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            updateChatsDocumentName();
        });
    }

    private void updateChatsDocumentName() {
        String chatName = Formatter.formatName(mGroupNameEditText.getEditableText().toString());
        if (chatName.equals(mChatName)) {
            mProgressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(mContext, getString(R.string.update_success), Toast.LENGTH_LONG).show();
            return;
        }
        mChatName = chatName;
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        batch.update(Utils.getChatsRef().document(mChatId), Constants.FIELD_NAME, mChatName);
        if (mAvatarHasChanged) {
            batch.update(Utils.getChatsRef().document(mChatId), Constants.FIELD_AVATAR_TIMESTAMP, Timestamp.now().toString());
        }
        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Utils.logTaskException(TAG, task);
                return;
            }
            String content = mCurrentUid + Constants.GROUP_NAME_CHANGED + mChatName;
            List<String> membersUid = mMembers.stream().map(Member::getUid).collect(Collectors.toList());
            Utils.addSystemChatsMessagesDocument(TAG, mChatId, Constants.CHAT_TYPE_GROUP, content, membersUid, () -> {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(mContext, getString(R.string.update_success), Toast.LENGTH_LONG).show();
            });
        });
    }
}
