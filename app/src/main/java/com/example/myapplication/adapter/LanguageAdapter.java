package com.example.myapplication.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.pojo.Language;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private final String mSelected;
    private final Context mContext;
    private final List<Language> mLanguages;
    private final OnItemClickListener mOnItemClickListener;

    public LanguageAdapter(Context context, List<Language> languages, OnItemClickListener onItemClickListener) {
        mContext = context;
        mLanguages = languages;
        mOnItemClickListener = onItemClickListener;

        String defaultLanguageCode = mContext.getString(R.string.pref_language_code_english);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSelected = sharedPreferences.getString(mContext.getString(R.string.pref_language_key), defaultLanguageCode);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.activity_sign_in_item, parent, false);
        return new LanguageAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Language language = mLanguages.get(position);
        if (language.getCode().equals(mSelected)) {
            Drawable check = AppCompatResources.getDrawable(mContext, R.drawable.ic_baseline_check_24);
            holder.mNameEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, check, null);
        }
        holder.mNameEditText.setText(language.getName());
        holder.mNameEditText.setOnClickListener(v -> mOnItemClickListener.onItemClick(language.getCode()));
    }

    @Override
    public int getItemCount() {
        return mLanguages.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String code);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextInputEditText mNameEditText;

        public ViewHolder(View itemView) {
            super(itemView);
            mNameEditText = itemView.findViewById(R.id.activity_sign_in_item_edit_text_language);
        }
    }
}
