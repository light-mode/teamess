package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;
import com.example.myapplication.document.UsersChatsDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Converter;
import com.example.myapplication.utilities.Utils;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    public static final String TAG = "ChatAdapter";

    public final Map<String, String> mChatNames;
    public final Map<String, String> mOthersUsername;

    private final String mCurrentUid;
    private final Context mContext;
    private final List<UsersChatsDocument> mUsersChatsDocuments;
    private final OnItemClickListener mOnItemClickListener;

    public ChatAdapter(Context context, List<UsersChatsDocument> usersChatsDocuments,
                       OnItemClickListener onItemClickListener) {
        mContext = context;
        mUsersChatsDocuments = usersChatsDocuments;
        mOnItemClickListener = onItemClickListener;

        mChatNames = new HashMap<>();
        mOthersUsername = new HashMap<>();
        mCurrentUid = Utils.getCurrentUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.fragment_chat_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsersChatsDocument usersChatsDocument = mUsersChatsDocuments.get(position);
        bindAvatar(usersChatsDocument, holder);
        bindChatName(usersChatsDocument, holder);
        bindLastMessage(usersChatsDocument, holder);
        holder.itemView.setOnClickListener(v -> mOnItemClickListener.onItemClick(
                usersChatsDocument.getId(), usersChatsDocument.getType(), usersChatsDocument.getOtherUid()));
    }

    private void bindChatName(@NonNull UsersChatsDocument usersChatsDocument, ViewHolder holder) {
        if (Constants.CHAT_TYPE_SINGLE.equals(usersChatsDocument.getType())) {
            String uid = usersChatsDocument.getOtherUid();
            holder.mChatNameTextView.setText(mOthersUsername.get(uid));
        } else {
            holder.mChatNameTextView.setText(mChatNames.get(usersChatsDocument.getId()));
        }
    }

    private void bindLastMessage(@NonNull UsersChatsDocument usersChatsDocument, ViewHolder holder) {
        String message;
        switch (usersChatsDocument.getLastMessageType()) {
            case Constants.MESSAGE_TYPE_TEXT:
                if (usersChatsDocument.isLastMessageDeleted()) {
                    message = mContext.getText(R.string.default_message_deleted).toString();
                } else {
                    message = usersChatsDocument.getLastMessageContent();
                }
                break;
            case Constants.MESSAGE_TYPE_ICON:
                message = mContext.getText(R.string.sent_an_icon).toString();
                break;
            case Constants.MESSAGE_TYPE_IMAGE:
                message = mContext.getText(R.string.sent_an_image).toString();
                break;
            case Constants.MESSAGE_TYPE_FILE:
                message = mContext.getText(R.string.sent_a_file).toString();
                break;
            case Constants.MESSAGE_TYPE_SYSTEM:
                String content = usersChatsDocument.getLastMessageContent();
                String[] contentArray = content.split("_");
                String uidSource = contentArray[0];
                if (content.contains(Constants.GROUP_CREATED)) {
                    message = mContext.getString(R.string.system_message_format_group_created,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                } else if (content.contains(Constants.USER_ADDED)) {
                    String uidTarget = contentArray[contentArray.length - 1];
                    message = mContext.getString(R.string.system_message_format_user_added,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                            uidTarget.equals(mCurrentUid) ? "you" : mOthersUsername.get(uidTarget));
                } else if (content.contains(Constants.USER_REMOVED)) {
                    String uidTarget = contentArray[contentArray.length - 1];
                    if (uidSource.equals(uidTarget)) {
                        message = mContext.getString(R.string.system_message_format_user_left,
                                uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                    } else {
                        message = mContext.getString(R.string.system_message_format_user_removed,
                                uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                                uidTarget.equals(mCurrentUid) ? "you" : mOthersUsername.get(uidTarget));
                    }
                } else if (content.contains(Constants.GROUP_AVATAR_CHANGED)) {
                    message = mContext.getString(R.string.system_message_format_group_avatar_changed,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                } else if (content.contains(Constants.GROUP_NAME_CHANGED)) {
                    String newGroupName = contentArray[contentArray.length - 1];
                    message = mContext.getString(R.string.system_message_format_group_name_changed,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                            newGroupName);
                } else {
                    throw new RuntimeException();
                }
                break;
            default:
                throw new RuntimeException();
        }
        if (Constants.MESSAGE_TYPE_SYSTEM.equals(usersChatsDocument.getLastMessageType())) {
            holder.mLastMessageTextView.setText(mContext.getString(R.string.last_message_system_format,
                    Converter.convertTimestampToString(usersChatsDocument.getLastMessageTimestamp()),
                    message));
            String content = usersChatsDocument.getLastMessageContent();
            if (content.contains(Constants.GROUP_CREATED) || content.contains(Constants.GROUP_NAME_CHANGED)) {
                String[] contentArray = content.split("_");
                String groupName = contentArray[contentArray.length - 1];
                holder.mChatNameTextView.setText(groupName);
            } else if (usersChatsDocument.getLastMessageContent().contains(Constants.GROUP_AVATAR_CHANGED)) {
                Glide.with(mContext).load(Utils.getChatsAvatarRef(usersChatsDocument.getId()))
                        .error(R.drawable.ic_baseline_group_24)
                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.mAvatarImageView);
            }
        } else {
            String senderUid = usersChatsDocument.getLastMessageSenderUid();
            holder.mLastMessageTextView.setText(mContext.getString(R.string.last_message_normal_format,
                    Converter.convertTimestampToString(usersChatsDocument.getLastMessageTimestamp()),
                    senderUid.equals(mCurrentUid) ? "You" : mOthersUsername.get(senderUid),
                    message
            ));
        }
    }

    private void bindAvatar(@NonNull UsersChatsDocument usersChatsDocument, ViewHolder holder) {
        holder.mAvatarImageView.setImageDrawable(null);
        StorageReference avatarRef = Constants.CHAT_TYPE_SINGLE.equals(usersChatsDocument.getType())
                ? Utils.getUsersAvatarRef(usersChatsDocument.getOtherUid())
                : Utils.getChatsAvatarRef(usersChatsDocument.getId());
        Glide.with(mContext).load(avatarRef)
                .error(Constants.CHAT_TYPE_SINGLE.equals(usersChatsDocument.getType())
                        ? R.drawable.ic_baseline_person_24
                        : R.drawable.ic_baseline_group_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.mAvatarImageView);
    }

    public interface OnItemClickListener {
        void onItemClick(String chatId, String chatType, String otherUid);
    }

    @Override
    public int getItemCount() {
        return mUsersChatsDocuments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAvatarImageView;
        private final TextView mChatNameTextView;
        private final TextView mLastMessageTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.fragment_chat_item_image_view_avatar);
            mChatNameTextView = itemView.findViewById(R.id.fragment_chat_item_text_view_chat_name);
            mLastMessageTextView = itemView.findViewById(R.id.fragment_chat_item_text_view_last_message);
        }
    }
}
