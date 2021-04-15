package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;
import com.example.myapplication.pojo.SearchResult;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private final Context mContext;
    private final List<SearchResult> mSearchResults;
    private final OnItemClickListener mOnItemClickListener;

    public SearchResultAdapter(Context context, List<SearchResult> searchResults, OnItemClickListener onItemClickListener) {
        mContext = context;
        mSearchResults = searchResults;
        mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.fragment_search_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult searchResult = mSearchResults.get(position);
        holder.mBioTextView.setText(searchResult.getBio());
        holder.mUsernameTextView.setText(searchResult.getUsername());
        Glide.with(mContext).load(searchResult.getAvatarRef())
                .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Drawable drawable = Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE);
                        holder.itemView.setOnClickListener(v -> mOnItemClickListener.onItemClick(
                                searchResult.getUid(), searchResult.getUsername(), drawable));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.itemView.setOnClickListener(v -> mOnItemClickListener.onItemClick(
                                searchResult.getUid(), searchResult.getUsername(), resource));
                        return false;
                    }
                })
                .into(holder.mAvatarImageView);
    }

    @Override
    public int getItemCount() {
        return mSearchResults.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String uid, String username, Drawable avatar);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mBioTextView;
        private final ImageView mAvatarImageView;
        private final TextView mUsernameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mBioTextView = itemView.findViewById(R.id.fragment_search_item_text_view_bio);
            mAvatarImageView = itemView.findViewById(R.id.fragment_search_item_image_view_avatar);
            mUsernameTextView = itemView.findViewById(R.id.fragment_search_item_text_view_username);
        }
    }
}
