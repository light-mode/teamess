package com.example.myapplication.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activity.GroupActivity;
import com.example.myapplication.activity.MainActivity;
import com.example.myapplication.activity.ViewAccountActivity;
import com.example.myapplication.adapter.SearchResultAdapter;
import com.example.myapplication.pojo.SearchResult;
import com.example.myapplication.utilities.Constants;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements SearchResultAdapter.OnItemClickListener {
    public static final String TAG = "SearchFragment";

    public TextView mTextView;
    public ProgressBar mProgressBar;
    public RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // attachToRoot set to false is required
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setReference(view);
        setDefault();
    }

    private void setReference(View view) {
        mTextView = view.findViewById(R.id.fragment_search_text_view);
        mProgressBar = view.findViewById(R.id.fragment_search_progress_bar);
        mRecyclerView = view.findViewById(R.id.fragment_search_recycler_view);
    }

    private void setDefault() {
        mTextView.setText(getString(R.string.fragment_search_text_view_text_default));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new SearchResultAdapter(getContext(), new ArrayList<>(), this));
    }

    public void populate(List<SearchResult> searchResults) {
        mRecyclerView.setAdapter(new SearchResultAdapter(getContext(), searchResults, this));
    }

    @Override
    public void onItemClick(String uid, String username, Drawable avatar) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (activity instanceof MainActivity) {
            Intent intent = new Intent(getActivity(), ViewAccountActivity.class);
            intent.putExtra(Constants.EXTRA_OTHER_UID, uid);
            startActivity(intent);
        } else if (activity instanceof GroupActivity) {
            ((GroupActivity) activity).onSearchResultSelect(uid, username, avatar);
        } else {
            throw new RuntimeException();
        }
    }
}
