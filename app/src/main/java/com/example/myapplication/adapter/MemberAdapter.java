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
import com.example.myapplication.pojo.Member;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
    private final Context mContext;
    private final List<Member> mMembers;

    public MemberAdapter(Context context, List<Member> members) {
        mContext = context;
        mMembers = members;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.fragment_group_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Member member = mMembers.get(position);
        holder.mMember = member;
        holder.mUsernameTextView.setText(member.getUsername());
        Glide.with(mContext).load(member.getAvatar())
                .error(Utils.getDefaultDrawable(mContext, Constants.DEFAULT_PERSON_AVATAR_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.mAvatarImageView);
    }

    @Override
    public int getItemCount() {
        return mMembers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Member mMember;
        public final ImageView mAvatarImageView;
        public final TextView mUsernameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.fragment_group_item_image_view_avatar);
            mUsernameTextView = itemView.findViewById(R.id.fragment_group_item_text_view_username);
        }
    }
}
