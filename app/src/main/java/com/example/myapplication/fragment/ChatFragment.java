package com.example.myapplication.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activity.ChatActivity;
import com.example.myapplication.adapter.ChatAdapter;
import com.example.myapplication.document.ChatsDocument;
import com.example.myapplication.document.UsersChatsDocument;
import com.example.myapplication.document.UsersDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatFragment extends Fragment implements ChatAdapter.OnItemClickListener {
    public static final String TAG = "ChatFragment";

    private TextView mTextView;

    private ChatAdapter mChatAdapter;
    private final String mCurrentUid;
    private final Map<String, String> mOthersUsername;
    private final List<UsersChatsDocument> mUsersChatsDocuments;
    private final ActivityResultLauncher<Intent> mActivityLauncher;

    public ChatFragment() {
        mCurrentUid = Utils.getCurrentUid();
        mOthersUsername = new HashMap<>();
        mUsersChatsDocuments = new ArrayList<>();
        mActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // attachToRoot set to false is required
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextView = view.findViewById(R.id.fragment_chat_text_view);
        mChatAdapter = new ChatAdapter(getContext(), mUsersChatsDocuments, this);
        RecyclerView mRecyclerView = view.findViewById(R.id.fragment_chat_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mChatAdapter);
        addUsersChatsListener();
    }

    private void addUsersChatsListener() {
        Utils.getUsersChatsRef(mCurrentUid).addSnapshotListener((value, error) -> {
            if (value == null) {
                Log.e(TAG, error == null ? "" : error.getMessage());
                return;
            }
            List<DocumentChange> documentChanges = value.getDocumentChanges();
            for (DocumentChange documentChange : documentChanges) {
                UsersChatsDocument usersChatsDocument = documentChange.getDocument().toObject(UsersChatsDocument.class);
                switch (usersChatsDocument.getType()) {
                    case Constants.CHAT_TYPE_SINGLE:
                        addSingleUsersChatsListener(usersChatsDocument, documentChange);
                        break;
                    case Constants.CHAT_TYPE_GROUP:
                        addGroupUsersChatsListener(usersChatsDocument, documentChange);
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
        });
    }

    private void addSingleUsersChatsListener(@NonNull UsersChatsDocument usersChatsDocument, DocumentChange documentChange) {
        String otherUid = usersChatsDocument.getOtherUid();
        Utils.getUsersRef().document(otherUid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Utils.logTaskException(TAG, task);
                return;
            }
            UsersDocument usersDocument = Objects.requireNonNull(task.getResult().toObject(UsersDocument.class));
            mOthersUsername.put(otherUid, usersDocument.getUsername());
            mChatAdapter.mOthersUsername.put(otherUid, usersDocument.getUsername());
            onUsersChatsDocumentChanged(usersChatsDocument, documentChange);
        });
    }

    private void addGroupUsersChatsListener(@NonNull UsersChatsDocument usersChatsDocument, DocumentChange documentChange) {
        String chatId = usersChatsDocument.getId();
        Utils.getChatsRef().document(chatId).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Utils.logTaskException(TAG, task);
                return;
            }
            ChatsDocument chatsDocument = Objects.requireNonNull(task.getResult().toObject(ChatsDocument.class));
            mChatAdapter.mChatNames.put(chatId, chatsDocument.getName());
            List<String> newUserIds = new ArrayList<>();
            for (String uid : chatsDocument.getMembersUid()) {
                if (uid.equals(mCurrentUid) || mOthersUsername.containsKey(uid)) {
                    continue;
                }
                newUserIds.add(uid);
            }
            if (newUserIds.isEmpty()) {
                onUsersChatsDocumentChanged(usersChatsDocument, documentChange);
                return;
            }
            Utils.getUsersRef().whereIn(FieldPath.documentId(), newUserIds).get().addOnCompleteListener(getNewUsernameTask -> {
                if (!getNewUsernameTask.isSuccessful() || getNewUsernameTask.getResult() == null) {
                    Utils.logTaskException(TAG, getNewUsernameTask);
                    return;
                }
                List<DocumentSnapshot> documentSnapshots = getNewUsernameTask.getResult().getDocuments();
                for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                    String uid = documentSnapshot.getId();
                    String username = Objects.requireNonNull(documentSnapshot.get(Constants.FIELD_USERNAME)).toString();
                    mOthersUsername.put(uid, username);
                    mChatAdapter.mOthersUsername.put(uid, username);
                }
                onUsersChatsDocumentChanged(usersChatsDocument, documentChange);
            });
        });
    }

    private void onUsersChatsDocumentChanged(UsersChatsDocument usersChatsDocument, @NonNull DocumentChange documentChange) {
        switch (documentChange.getType()) {
            case ADDED:
                onUsersChatsDocumentAdded(usersChatsDocument);
                break;
            case MODIFIED:
                onUsersChatsDocumentModified(usersChatsDocument);
                break;
            case REMOVED:
                onUsersChatsDocumentRemoved(usersChatsDocument);
                break;
        }
    }

    private void onUsersChatsDocumentAdded(UsersChatsDocument usersChatsDocument) {
        mUsersChatsDocuments.add(usersChatsDocument);
        mUsersChatsDocuments.sort((o1, o2) -> -o1.getLastMessageTimestamp().compareTo(o2.getLastMessageTimestamp()));
        mChatAdapter.notifyDataSetChanged();
        if (View.VISIBLE == mTextView.getVisibility()) {
            mTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void onUsersChatsDocumentModified(UsersChatsDocument usersChatsDocument) {
        UsersChatsDocument displaying = mUsersChatsDocuments.stream()
                .filter(o -> o.getId().equals(usersChatsDocument.getId())).collect(Collectors.toList()).get(0);
        int position = mUsersChatsDocuments.indexOf(displaying);
        if (position == 0) {
            mUsersChatsDocuments.set(position, usersChatsDocument);
            mChatAdapter.notifyItemChanged(position);
        } else {
            mUsersChatsDocuments.remove(position);
            mUsersChatsDocuments.add(0, usersChatsDocument);
            mChatAdapter.notifyItemRemoved(position);
            mChatAdapter.notifyItemRangeChanged(position, mUsersChatsDocuments.size());
            mChatAdapter.notifyItemInserted(0);
        }
    }

    private void onUsersChatsDocumentRemoved(@NonNull UsersChatsDocument usersChatsDocument) {
        UsersChatsDocument displaying = mUsersChatsDocuments.stream()
                .filter(o -> o.getId().equals(usersChatsDocument.getId())).collect(Collectors.toList()).get(0);
        int position = mUsersChatsDocuments.indexOf(displaying);
        mUsersChatsDocuments.remove(position);
        mChatAdapter.notifyItemRemoved(position);
        mChatAdapter.notifyItemRangeChanged(position, mUsersChatsDocuments.size());
        if (mUsersChatsDocuments.isEmpty()) {
            mTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(String chatId, String chatType, String otherUid) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(Constants.EXTRA_AUTHENTICATED, true);
        intent.putExtra(Constants.EXTRA_CHAT_ID, chatId);
        intent.putExtra(Constants.EXTRA_CHAT_TYPE, chatType);
        intent.putExtra(Constants.EXTRA_OTHER_UID, otherUid);
        intent.putExtra(Constants.EXTRA_OTHERS_USERNAME, (Serializable) mOthersUsername);
        mActivityLauncher.launch(intent);
    }
}
