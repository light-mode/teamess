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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public static final String TAG = "MessageAdapter";
    public int memberCount;

    private TextView mDisplayingTimestampTextView;
    private TextView mDisplayingSeenTextView;
    private FloatingActionButton mDisplayingCopyButton;
    private FloatingActionButton mDisplayingDownloadButton;
    private FloatingActionButton mDisplayingDeleteButton;
    private FloatingActionButton mDisplayingFullscreenButton;

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
            holder.mTimestampTextView.setText(Converter.convertTimestampToString(mContext, chatsMessagesDocument.getTimestamp()));
        }
        if (Constants.MESSAGE_TYPE_SYSTEM.equals(chatsMessagesDocument.getType())) {
            loadUsernameThenBindMainView(chatsMessagesDocument, holder);
            return;
        }
        bindAvatarImageView(chatsMessagesDocument, holder);
        loadUsernameThenBindSeenViewAndMainView(chatsMessagesDocument, holder);
    }

    private void loadUsernameThenBindSeenViewAndMainView(@NonNull ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder) {
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

    private void bindAvatarImageView(@NonNull ChatsMessagesDocument chatsMessagesDocument, @NonNull ViewHolder holder) {
        if (holder.mAvatarImageView == null) {
            return;
        }
        String senderUid = chatsMessagesDocument.getSenderUid();
        Glide.with(mContext).load(Utils.getUsersAvatarRef(senderUid))
                .error(R.drawable.ic_baseline_person_24)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.mAvatarImageView);
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
            if (seenUserIds.size() == memberCount) {
                seen.append(mContext.getString(R.string.seen_by_everyone));
            } else {
                seen.append(mContext.getString(R.string.seen_by)).append(" ");
                for (int index = 0; index < seenUserIds.size(); index++) {
                    String seenUid = seenUserIds.get(index);
                    seen.append(seenUid.equals(mCurrentUid) ? mContext.getString(R.string.you) : mOthersUsername.get(seenUid));
                    seen.append(index == seenUserIds.size() - 1 ? "" : ", ");
                }
            }
        }
        seenTextView.setText(seen.toString());
    }

    private void loadUsernameThenBindMainView(@NonNull ChatsMessagesDocument chatsMessagesDocument, @NonNull ViewHolder holder) {
        String content = chatsMessagesDocument.getContent();
        if (holder.getItemViewType() == VIEW_TYPE_SYSTEM) {
            if (content.contains(Constants.USER_ADDED) || content.contains(Constants.USER_REMOVED)) {
                String[] contentArray = content.split("_");
                String sourceUid = contentArray[0];
                String targetUid = contentArray[contentArray.length - 1];
                List<String> newUserIds = new ArrayList<>();
                if (mOthersUsername.get(sourceUid) == null) {
                    newUserIds.add(sourceUid);
                }
                if (mOthersUsername.get(targetUid) == null) {
                    newUserIds.add(targetUid);
                }
                if (newUserIds.isEmpty()) {
                    bindMainView(chatsMessagesDocument, holder);
                    return;
                }
                Utils.getUsersRef().whereIn(FieldPath.documentId(), newUserIds).get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Utils.logTaskException(TAG, task);
                        return;
                    }
                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                    for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                        String uid = documentSnapshot.getId();
                        String username = Objects.requireNonNull(documentSnapshot.get(Constants.FIELD_USERNAME)).toString();
                        mOthersUsername.put(uid, username);
                    }
                    bindMainView(chatsMessagesDocument, holder);
                });
            } else if (content.contains(Constants.GROUP_CREATED)
                    || content.contains(Constants.GROUP_NAME_CHANGED)
                    || content.contains(Constants.GROUP_AVATAR_CHANGED)) {
                String[] contentArray = content.split("_");
                String creatorUid = contentArray[0];
                if (mOthersUsername.get(creatorUid) != null) {
                    bindMainView(chatsMessagesDocument, holder);
                    return;
                }
                Utils.getUsersRef().document(creatorUid).get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Utils.logTaskException(TAG, task);
                        return;
                    }
                    String username = Objects.requireNonNull(task.getResult().get(Constants.FIELD_USERNAME)).toString();
                    mOthersUsername.put(creatorUid, username);
                    bindMainView(chatsMessagesDocument, holder);
                });
            } else if (content.contains(Constants.BLOCKED) || content.contains(Constants.UNBLOCKED)) {
                bindMainView(chatsMessagesDocument, holder);
            } else {
                throw new RuntimeException();
            }
        } else {
            bindMainView(chatsMessagesDocument, holder);
        }
    }

    private void bindMainView(ChatsMessagesDocument chatsMessagesDocument, @NonNull ViewHolder holder) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_TEXT_LEFT:
            case VIEW_TYPE_TEXT_RIGHT:
                if (holder.mContentTextView == null) {
                    break;
                }
                if (chatsMessagesDocument.isDeleted()) {
                    holder.mContentTextView.setText(mContext.getText(R.string.default_message_deleted));
                    holder.mContentTextView.setTextColor(Color.GRAY);
                    holder.mContentTextView.setClickable(false);
                    break;
                }
                holder.mContentTextView.setText(chatsMessagesDocument.getContent());
                holder.mContentTextView.setTextColor(ContextCompat.getColor(mContext,
                        chatsMessagesDocument.getSenderUid().equals(mCurrentUid)
                                ? R.color.black : R.color.design_default_color_on_primary));
                holder.mContentTextView.setOnClickListener(v -> onPrimaryViewClick(chatsMessagesDocument, holder));
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
                holder.mContentIconView.setOnClickListener(v -> onPrimaryViewClick(chatsMessagesDocument, holder));
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
                                holder.mContentImageView.setOnClickListener(v -> onImageViewClick(chatsMessagesDocument, holder, imageId, resource));
                                return false;
                            }
                        }).into(holder.mContentImageView);
                break;
            case VIEW_TYPE_FILE_LEFT:
            case VIEW_TYPE_FILE_RIGHT:
                if (holder.mContentTextView != null) {
                    holder.mContentTextView.setText(chatsMessagesDocument.getContent());
                    holder.mContentTextView.setOnClickListener(v -> onPrimaryViewClick(chatsMessagesDocument, holder));
                }
                break;
            case VIEW_TYPE_SYSTEM:
                if (holder.mContentTextView == null) {
                    break;
                }
                String you = mContext.getString(R.string.you);
                String capitalYou = mContext.getString(R.string.you_capital);
                String message = Converter.convertTimestampToString(mContext, chatsMessagesDocument.getTimestamp()) + '\n';
                String content = chatsMessagesDocument.getContent();
                String[] contentArray = content.split("_");
                String sourceUid = contentArray[0];
                if (content.contains(Constants.GROUP_CREATED)) {
                    message += mContext.getString(R.string.system_message_format_group_created,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid));
                } else if (content.contains(Constants.USER_ADDED)) {
                    String targetUid = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_user_added,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid),
                            targetUid.equals(mCurrentUid) ? you : mOthersUsername.get(targetUid));
                } else if (content.contains(Constants.USER_REMOVED)) {
                    String targetUid = contentArray[contentArray.length - 1];
                    if (sourceUid.equals(targetUid)) {
                        message += mContext.getString(R.string.system_message_format_user_left,
                                sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid));
                    } else {
                        message += mContext.getString(R.string.system_message_format_user_removed,
                                sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid),
                                targetUid.equals(mCurrentUid) ? you : mOthersUsername.get(targetUid));
                    }
                } else if (content.contains(Constants.GROUP_NAME_CHANGED)) {
                    String newGroupName = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_group_name_changed,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid),
                            newGroupName);
                } else if (content.contains(Constants.GROUP_AVATAR_CHANGED)) {
                    message += mContext.getString(R.string.system_message_format_group_avatar_changed,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid));
                } else if (content.contains(Constants.BLOCKED)) {
                    String targetUid = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_blocked,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid),
                            targetUid.equals(mCurrentUid) ? you : mOthersUsername.get(targetUid));
                } else if (content.contains(Constants.UNBLOCKED)) {
                    String targetUid = contentArray[contentArray.length - 1];
                    message += mContext.getString(R.string.system_message_format_unblocked,
                            sourceUid.equals(mCurrentUid) ? capitalYou : mOthersUsername.get(sourceUid),
                            targetUid.equals(mCurrentUid) ? you : mOthersUsername.get(targetUid));
                } else {
                    throw new RuntimeException();
                }
                holder.mContentTextView.setText(message);
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void onPrimaryViewClick(ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder) {
        hideDisplayingSecondaryView();
        showSecondaryViewFromSelectedPrimaryView(chatsMessagesDocument, holder);
    }

    private void onImageViewClick(ChatsMessagesDocument chatsMessagesDocument, ViewHolder holder, String imageId, Drawable drawable) {
        hideDisplayingSecondaryView();
        showSecondaryViewsFromSelectedImageView(chatsMessagesDocument, holder, imageId, drawable);
    }

    public void hideDisplayingSecondaryView() {
        if (mDisplayingTimestampTextView != null) {
            mDisplayingTimestampTextView.setVisibility(View.GONE);
            mDisplayingTimestampTextView = null;
        }
        if (mDisplayingSeenTextView != null) {
            mDisplayingSeenTextView.setVisibility(View.GONE);
            mDisplayingSeenTextView = null;
        }
        if (mDisplayingCopyButton != null) {
            mDisplayingCopyButton.setVisibility(View.GONE);
            mDisplayingCopyButton = null;
        }
        if (mDisplayingDownloadButton != null) {
            mDisplayingDownloadButton.setVisibility(View.GONE);
            mDisplayingDownloadButton = null;
        }
        if (mDisplayingDeleteButton != null) {
            mDisplayingDeleteButton.setVisibility(View.GONE);
            mDisplayingDeleteButton = null;
        }
        if (mDisplayingFullscreenButton != null) {
            mDisplayingFullscreenButton.setVisibility(View.GONE);
            mDisplayingFullscreenButton = null;
        }
    }

    private void showSecondaryViewFromSelectedPrimaryView(ChatsMessagesDocument chatsMessagesDocument, @NonNull ViewHolder holder) {
        if (holder.mTimestampTextView != null) {
            holder.mTimestampTextView.setVisibility(View.VISIBLE);
            mDisplayingTimestampTextView = holder.mTimestampTextView;
        }
        if (holder.mSeenTextView != null) {
            holder.mSeenTextView.setVisibility(View.VISIBLE);
            mDisplayingSeenTextView = holder.mSeenTextView;
        }
        if (holder.mCopyButton != null) {
            holder.mCopyButton.setVisibility(View.VISIBLE);
            holder.mCopyButton.setOnClickListener(v -> onCopyButtonClick(chatsMessagesDocument.getContent()));
            mDisplayingCopyButton = holder.mCopyButton;
        }
        if (holder.mDownloadButton != null) {
            holder.mDownloadButton.setVisibility(View.VISIBLE);
            holder.mDownloadButton.setOnClickListener(v -> mOnClickListener.onDownloadButtonClick(chatsMessagesDocument));
            mDisplayingDownloadButton = holder.mDownloadButton;
        }
        if (holder.mDeleteButton != null) {
            holder.mDeleteButton.setVisibility(View.VISIBLE);
            holder.mDeleteButton.setOnClickListener(v -> mOnClickListener.onDeleteButtonClick(chatsMessagesDocument));
            mDisplayingDeleteButton = holder.mDeleteButton;
        }
    }

    private void showSecondaryViewsFromSelectedImageView(ChatsMessagesDocument chatsMessagesDocument, @NonNull ViewHolder holder, String imageId, Drawable image) {
        if (holder.mTimestampTextView != null) {
            holder.mTimestampTextView.setVisibility(View.VISIBLE);
            mDisplayingTimestampTextView = holder.mTimestampTextView;
        }
        if (holder.mSeenTextView != null) {
            holder.mSeenTextView.setVisibility(View.VISIBLE);
            mDisplayingSeenTextView = holder.mSeenTextView;
        }
        if (holder.mDownloadButton != null) {
            holder.mDownloadButton.setVisibility(View.VISIBLE);
            holder.mDownloadButton.setOnClickListener(v -> mOnClickListener.onDownloadButtonClick(chatsMessagesDocument));
            mDisplayingDownloadButton = holder.mDownloadButton;
        }
        if (holder.mFullscreenButton != null) {
            holder.mFullscreenButton.setVisibility(View.VISIBLE);
            holder.mFullscreenButton.setOnClickListener(v -> mOnClickListener.onFullscreenButtonClick(imageId, image));
            mDisplayingFullscreenButton = holder.mFullscreenButton;
        }
    }

    private void onCopyButtonClick(String content) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(content, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(mContext, mContext.getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
    }

    public interface OnClickListener {
        void onFullscreenButtonClick(String imageId, Drawable image);

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
        private final FloatingActionButton mFullscreenButton;

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
            mFullscreenButton = itemView.findViewById(R.id.activity_chat_item_button_fullscreen);
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
