package com.example.myapplication.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;
import com.example.myapplication.document.ChatsMessagesDocument;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Converter;
import com.example.myapplication.utilities.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public static final String TAG = "MessageAdapter";

    private final String mChatId;
    private final String mChatType;
    private final Context mContext;
    private final String mCurrentUid;
    private final OnClickListener mOnClickListener;
    private final Map<String, String> mOthersUsername;
    private final List<ChatsMessagesDocument> mChatsMessagesDocuments;

    private static final int VIEW_TYPE_TEXT_LEFT = 0;
    private static final int VIEW_TYPE_TEXT_RIGHT = 1;
    private static final int VIEW_TYPE_ICON_LEFT = 2;
    private static final int VIEW_TYPE_ICON_RIGHT = 3;
    private static final int VIEW_TYPE_IMAGE_LEFT = 4;
    private static final int VIEW_TYPE_IMAGE_RIGHT = 5;
    private static final int VIEW_TYPE_FILE_LEFT = 6;
    private static final int VIEW_TYPE_FILE_RIGHT = 7;
    private static final int VIEW_TYPE_SYSTEM = 8;

    public MessageAdapter(Context context, String chatId, String chatType,
                          List<ChatsMessagesDocument> chatsMessagesDocuments,
                          Map<String, String> othersUsername, OnClickListener onClickListener) {
        mCurrentUid = Utils.getCurrentUid();

        mContext = context;
        mChatId = chatId;
        mChatType = chatType;
        mChatsMessagesDocuments = chatsMessagesDocuments;
        mOthersUsername = othersUsername;
        mOnClickListener = onClickListener;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view;
        switch (viewType) {
            case VIEW_TYPE_TEXT_LEFT:
                view = inflater.inflate(R.layout.activity_chat_item_text_left, parent, false);
                break;
            case VIEW_TYPE_TEXT_RIGHT:
                view = inflater.inflate(R.layout.activity_chat_item_text_right, parent, false);
                break;
            case VIEW_TYPE_ICON_LEFT:
                view = inflater.inflate(R.layout.activity_chat_item_icon_left, parent, false);
                break;
            case VIEW_TYPE_ICON_RIGHT:
                view = inflater.inflate(R.layout.activity_chat_item_icon_right, parent, false);
                break;
            case VIEW_TYPE_IMAGE_LEFT:
                view = inflater.inflate(R.layout.activity_chat_item_image_left, parent, false);
                break;
            case VIEW_TYPE_IMAGE_RIGHT:
                view = inflater.inflate(R.layout.activity_chat_item_image_right, parent, false);
                break;
            case VIEW_TYPE_FILE_LEFT:
                view = inflater.inflate(R.layout.activity_chat_item_file_left, parent, false);
                break;
            case VIEW_TYPE_FILE_RIGHT:
                view = inflater.inflate(R.layout.activity_chat_item_file_right, parent, false);
                break;
            case VIEW_TYPE_SYSTEM:
                view = inflater.inflate(R.layout.activity_chat_item_system, parent, false);
                break;
            default:
                throw new RuntimeException();
        }
        return new MessageAdapter.ViewHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mAvatarImageView != null) {
            holder.mAvatarImageView.setImageBitmap(null);
        }
        if (holder.mContentImageView != null) {
            holder.mContentImageView.setImageBitmap(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        if (holder.mAvatarImageView != null) {
            holder.mAvatarImageView.setImageDrawable(null);
        }
        if (holder.mContentImageView != null) {
            holder.mContentImageView.setImageDrawable(null);
        }
        ChatsMessagesDocument chatsMessagesDocument = mChatsMessagesDocuments.get(position);
        if (holder.mTimestampTextView != null) {
            holder.mTimestampTextView.setText(Converter.convertTimestampToString(chatsMessagesDocument.getTimestamp()));
        }
        if (Constants.MESSAGE_TYPE_SYSTEM.equals(chatsMessagesDocument.getType())) {
            loadUsernameThenBindMainView(chatsMessagesDocument, holder);
            return;
        }
        bindAvatarImageView(chatsMessagesDocument, holder.mAvatarImageView);
        loadUsernameThenBindSeenViewAndMainView(chatsMessagesDocument, holder);
    }

    private void loadUsernameThenBindSeenViewAndMainView(ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder) {
        String senderUid = chatsMessagesDocument.getSenderUid();
        String senderUsername = mOthersUsername.get(senderUid);
        if (!senderUid.equals(mCurrentUid) && senderUsername == null) {
            Utils.getUsersRef().document(senderUid).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Utils.logTaskException(TAG, task);
                    return;
                }
                String username = Objects.requireNonNull(task.getResult().get(Constants.FIELD_USERNAME)).toString();
                mOthersUsername.put(senderUid, username);
                bindSeenTextView(chatsMessagesDocument, holder.mSeenTextView);
                loadUsernameThenBindMainView(chatsMessagesDocument, holder);
            });
        } else {
            bindSeenTextView(chatsMessagesDocument, holder.mSeenTextView);
            loadUsernameThenBindMainView(chatsMessagesDocument, holder);
        }
    }

    private void bindAvatarImageView(ChatsMessagesDocument chatsMessagesDocument, ImageView avatarImageView) {
        if (avatarImageView == null) {
            return;
        }
        String senderUid = chatsMessagesDocument.getSenderUid();
        Glide.with(mContext).load(Utils.getUsersAvatarRef(senderUid))
                .error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                }).into(avatarImageView);
    }

    private void bindSeenTextView(ChatsMessagesDocument chatsMessagesDocument, TextView seenTextView) {
        if (seenTextView == null) {
            return;
        }
        List<String> seenUserIds = chatsMessagesDocument.getSeenUserIds();
        StringBuilder seen = new StringBuilder();
        if (Constants.CHAT_TYPE_SINGLE.equals(mChatType)) {
            if (seenUserIds.size() == 2) {
                seen.append(mContext.getString(R.string.seen));
            } else {
                seen.append(mContext.getString(R.string.delivered));
            }
        } else {
            if (seenUserIds.size() == mOthersUsername.size() + 1) {
                seen.append(mContext.getString(R.string.seen_by_everyone));
            } else {
                seen.append(mContext.getString(R.string.seen_by)).append(" ");
                for (int index = 0; index < seenUserIds.size(); index++) {
                    String seenUid = seenUserIds.get(index);
                    seen.append(seenUid.equals(mCurrentUid) ? "you" : mOthersUsername.get(seenUid));
                    seen.append(index == seenUserIds.size() - 1 ? "" : ", ");
                }
            }
        }
        seenTextView.setText(seen.toString());
    }

    private void loadUsernameThenBindMainView(ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder) {
        String content = chatsMessagesDocument.getContent();
        if (holder.getItemViewType() == VIEW_TYPE_SYSTEM && content.contains(Constants.USER_ADDED)) {
            String[] contentArray = content.split("_");
            String addedUid = contentArray[contentArray.length - 1];
            Utils.getUsersRef().document(addedUid).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Utils.logTaskException(TAG, task);
                    return;
                }
                String username = Objects.requireNonNull(task.getResult().get(Constants.FIELD_USERNAME)).toString();
                mOthersUsername.put(addedUid, username);
                bindMainView(chatsMessagesDocument, holder);
            });
        } else {
            bindMainView(chatsMessagesDocument, holder);
        }
    }

    private void bindMainView(ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_TEXT_LEFT:
            case VIEW_TYPE_TEXT_RIGHT:
                if (chatsMessagesDocument.isDeleted()) {
                    if (holder.mContentTextView != null) {
                        holder.mContentTextView.setText(mContext.getText(R.string.default_message_deleted));
                        holder.mContentTextView.setTextColor(Color.GRAY);
                    }
                    if (holder.mCopyButton != null) {
                        holder.mCopyButton.setVisibility(View.INVISIBLE);
                    }
                    if (holder.mDeleteButton != null) {
                        holder.mDeleteButton.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (holder.mContentTextView != null) {
                        holder.mContentTextView.setText(chatsMessagesDocument.getContent());
                        holder.mContentTextView.setTextColor(ContextCompat.getColor(mContext,
                                chatsMessagesDocument.getSenderUid().equals(mCurrentUid)
                                        ? R.color.black : R.color.design_default_color_on_primary));
                    }
                    if (holder.mCopyButton != null) {
                        holder.mCopyButton.setVisibility(View.VISIBLE);
                        holder.mCopyButton.setOnClickListener(v -> onCopyButtonClick(chatsMessagesDocument.getContent()));
                    }
                    if (holder.mDeleteButton != null) {
                        holder.mDeleteButton.setVisibility(View.VISIBLE);
                        holder.mDeleteButton.setOnClickListener(v -> mOnClickListener.onDeleteButtonClick(chatsMessagesDocument));
                    }
                }
                break;
            case VIEW_TYPE_ICON_LEFT:
            case VIEW_TYPE_ICON_RIGHT:
                if (holder.mContentIconView == null) {
                    break;
                }
                int icon;
                try {
                    icon = Integer.parseInt(chatsMessagesDocument.getContent());
                } catch (NumberFormatException e) {
                    break;
                }
                switch (icon) {
                    case Constants.ICON_VERY_SATISFIED:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_very_satisfied_24);
                        break;
                    case Constants.ICON_SATISFIED_ALT:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_satisfied_alt_24);
                        break;
                    case Constants.ICON_SATISFIED:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_satisfied_24);
                        break;
                    case Constants.ICON_NEUTRAL:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_neutral_24);
                        break;
                    case Constants.ICON_DISSATISFIED:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_dissatisfied_24);
                        break;
                    case Constants.ICON_VERY_DISSATISFIED:
                        holder.mContentIconView.setImageResource(R.drawable.ic_baseline_sentiment_very_dissatisfied_24);
                        break;
                }
                break;
            case VIEW_TYPE_IMAGE_LEFT:
            case VIEW_TYPE_IMAGE_RIGHT:
                if (holder.mContentImageView == null) {
                    break;
                }
                String imageId = chatsMessagesDocument.getContent();
                Glide.with(mContext).load(Utils.getChatsFileRef(mChatId, imageId))
                        .error(R.drawable.ic_baseline_broken_image_24)
                        .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                holder.mContentImageView.setOnClickListener(v -> mOnClickListener.onImageContentClick(imageId, resource));
                                return false;
                            }
                        }).into(holder.mContentImageView);
                if (holder.mDownloadButton != null) {
                    holder.mDownloadButton.setOnClickListener(v -> mOnClickListener.onDownloadButtonClick(chatsMessagesDocument));
                }
                break;
            case VIEW_TYPE_FILE_LEFT:
            case VIEW_TYPE_FILE_RIGHT:
                if (holder.mContentTextView != null) {
                    holder.mContentTextView.setText(chatsMessagesDocument.getContent());
                }
                if (holder.mDownloadButton != null) {
                    holder.mDownloadButton.setOnClickListener(
                            v -> mOnClickListener.onDownloadButtonClick(chatsMessagesDocument));
                }
                break;
            case VIEW_TYPE_SYSTEM:
                if (holder.mContentTextView == null) {
                    break;
                }
                String message = Converter.convertTimestampToString(chatsMessagesDocument.getTimestamp()) + '\n';
                String content = chatsMessagesDocument.getContent();
                String[] contentArray = content.split("_");
                String uidSource = contentArray[0];
                if (content.contains(Constants.GROUP_CREATED)) {
                    message += mContext.getString(R.string.system_message_format_group_created,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                } else if (content.contains(Constants.USER_ADDED)) {
                    String uidTarget = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_user_added,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                            uidTarget.equals(mCurrentUid) ? "you" : mOthersUsername.get(uidTarget));
                } else if (content.contains(Constants.USER_REMOVED)) {
                    String uidTarget = contentArray[contentArray.length - 1];
                    if (uidSource.equals(uidTarget)) {
                        message += mContext.getString(R.string.system_message_format_user_left,
                                uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                    } else {
                        message += mContext.getString(R.string.system_message_format_user_removed,
                                uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                                uidTarget.equals(mCurrentUid) ? "you" : mOthersUsername.get(uidTarget));
                    }
                } else if (content.contains(Constants.GROUP_NAME_CHANGED)) {
                    String newGroupName = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_group_name_changed,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource),
                            newGroupName);
                } else if (content.contains(Constants.GROUP_AVATAR_CHANGED)) {
                    message += mContext.getString(R.string.system_message_format_group_avatar_changed,
                            uidSource.equals(mCurrentUid) ? "You" : mOthersUsername.get(uidSource));
                } else {
                    throw new RuntimeException();
                }
                holder.mContentTextView.setText(message);
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void onCopyButtonClick(String content) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(content, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(mContext, mContext.getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
    }

    public interface OnClickListener {
        void onImageContentClick(String imageId, Drawable image);

        void onDownloadButtonClick(ChatsMessagesDocument chatsMessagesDocument);

        void onDeleteButtonClick(ChatsMessagesDocument chatsMessagesDocument);
    }

    @Override
    public int getItemCount() {
        return mChatsMessagesDocuments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAvatarImageView;
        private final TextView mTimestampTextView;
        private final TextView mSeenTextView;
        private final TextView mContentTextView;
        private final FloatingActionButton mContentIconView;
        private final ImageView mContentImageView;
        private final FloatingActionButton mCopyButton;
        private final FloatingActionButton mDownloadButton;
        private final FloatingActionButton mDeleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.activity_chat_item_image_view_avatar);
            mTimestampTextView = itemView.findViewById(R.id.activity_chat_item_text_view_timestamp);
            mSeenTextView = itemView.findViewById(R.id.activity_chat_item_text_view_seen);
            mContentTextView = itemView.findViewById(R.id.activity_chat_item_text_view_content);
            mContentIconView = itemView.findViewById(R.id.activity_chat_item_icon_view_content);
            mContentImageView = itemView.findViewById(R.id.activity_chat_item_image_view_content);
            mCopyButton = itemView.findViewById(R.id.activity_chat_item_button_copy);
            mDownloadButton = itemView.findViewById(R.id.activity_chat_item_button_download);
            mDeleteButton = itemView.findViewById(R.id.activity_chat_item_button_delete);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatsMessagesDocument chatsMessagesDocument = mChatsMessagesDocuments.get(position);
        String type = chatsMessagesDocument.getType();
        String senderUid = chatsMessagesDocument.getSenderUid();
        switch (type) {
            case Constants.MESSAGE_TYPE_TEXT:
                return senderUid.equals(mCurrentUid) ? VIEW_TYPE_TEXT_RIGHT : VIEW_TYPE_TEXT_LEFT;
            case Constants.MESSAGE_TYPE_ICON:
                return senderUid.equals(mCurrentUid) ? VIEW_TYPE_ICON_RIGHT : VIEW_TYPE_ICON_LEFT;
            case Constants.MESSAGE_TYPE_IMAGE:
                return senderUid.equals(mCurrentUid) ? VIEW_TYPE_IMAGE_RIGHT : VIEW_TYPE_IMAGE_LEFT;
            case Constants.MESSAGE_TYPE_FILE:
                return senderUid.equals(mCurrentUid) ? VIEW_TYPE_FILE_RIGHT : VIEW_TYPE_FILE_LEFT;
            case Constants.MESSAGE_TYPE_SYSTEM:
                return VIEW_TYPE_SYSTEM;
            default:
                throw new RuntimeException();
        }
    }
}
