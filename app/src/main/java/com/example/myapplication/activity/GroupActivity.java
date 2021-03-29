package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.R;
import com.example.myapplication.document.UsersDocument;
import com.example.myapplication.fragment.GroupFragment;
import com.example.myapplication.fragment.SearchFragment;
import com.example.myapplication.pojo.SearchResult;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Formatter;
import com.example.myapplication.utilities.Utils;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class GroupActivity extends BaseActivity {
    public static final String TAG = "GroupActivity";

    private SearchView mSearchView;
    private GroupFragment mGroupFragment;
    private SearchFragment mSearchFragment;

    private String mMode;
    private final String mCurrentUid;

    public GroupActivity() {
        mCurrentUid = Utils.getCurrentUid();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        setReference();
        setDefault();
    }

    private void setReference() {
        mMode = getIntent().getStringExtra(Constants.EXTRA_MODE);
    }

    private void setDefault() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        GroupFragment groupFragment = new GroupFragment();
        Bundle args = new Bundle();
        Intent intent = getIntent();
        args.putString(Constants.KEY_MODE, mMode);
        args.putString(Constants.KEY_CHAT_ID, intent.getStringExtra(Constants.EXTRA_CHAT_ID));
        args.putSerializable(Constants.KEY_OTHERS_USERNAME, intent.getSerializableExtra(Constants.EXTRA_OTHERS_USERNAME));
        groupFragment.setArguments(args);
        transaction.add(R.id.activity_group_fragment_container_view_group, groupFragment, GroupFragment.TAG);
        transaction.runOnCommit(() -> mGroupFragment = (GroupFragment) getSupportFragmentManager().
                findFragmentById(R.id.activity_group_fragment_container_view_group));
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Constants.MODE_CREATE_GROUP.equals(mMode) || Constants.MODE_ADD_MEMBER.equals(mMode)) {
            getMenuInflater().inflate(R.menu.menu_group, menu);
            MenuItem menuItem = menu.findItem(R.id.menu_group_item_search);
            mSearchView = (SearchView) menuItem.getActionView();
            mSearchView.setOnSearchClickListener(this::onSearchClick);
            mSearchView.setOnCloseListener(this::onSearchCloseIconClick);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    String queryText = Formatter.formatName(s).toLowerCase();
                    if (queryText.isEmpty()) {
                        mSearchFragment.populate(new ArrayList<>());
                        mSearchFragment.mTextView.setText(getString(R.string.fragment_search_text_view_text_default));
                        mSearchFragment.mTextView.setVisibility(View.VISIBLE);
                    } else {
                        new SearchTask().execute(queryText);
                    }
                    return true;
                }
            });
        }
        return true;
    }

    public class SearchTask extends AsyncTask<String, Void, List<SearchResult>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSearchFragment.mTextView.setVisibility(View.INVISIBLE);
            mSearchFragment.mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<SearchResult> doInBackground(@NonNull String... strings) {
            Semaphore semaphore = new Semaphore(0);
            List<SearchResult> searchResults = new ArrayList<>();
            String queryText = strings[0];
            Query query = Utils.getUsersRef()
                    .orderBy(Constants.FIELD_USERNAME_LOWERCASE)
                    .startAt(queryText).endAt(queryText + "\uf8ff");
            query.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Utils.logTaskException(TAG, task);
                    return;
                }
                for (QueryDocumentSnapshot document : task.getResult()) {
                    UsersDocument usersDocument = document.toObject(UsersDocument.class);
                    String uid = document.getId();
                    if (uid.equals(mCurrentUid)) {
                        continue;
                    }
                    String bio = usersDocument.getBio();
                    String username = usersDocument.getUsername();
                    StorageReference avatarRef = Utils.getUsersAvatarRef(uid);
                    SearchResult searchResult = new SearchResult(uid, bio, username, avatarRef);
                    searchResults.add(searchResult);
                }
                semaphore.release();
            });
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
            return searchResults;
        }

        @Override
        protected void onPostExecute(List<SearchResult> searchResults) {
            super.onPostExecute(searchResults);
            if (searchResults.isEmpty()) {
                mSearchFragment.populate(new ArrayList<>());
                mSearchFragment.mProgressBar.setVisibility(View.INVISIBLE);
                mSearchFragment.mTextView.setText(getString(R.string.fragment_search_text_view_text_no_result));
                mSearchFragment.mTextView.setVisibility(View.VISIBLE);
            } else {
                mSearchFragment.mProgressBar.setVisibility(View.INVISIBLE);
                mSearchFragment.mTextView.setVisibility(View.INVISIBLE);
                mSearchFragment.populate(searchResults);
            }
        }
    }

    private void onSearchClick(View v) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment groupFragment = fragmentManager.findFragmentById(R.id.activity_group_fragment_container_view_group);
        Fragment searchFragment = fragmentManager.findFragmentById(R.id.activity_group_fragment_container_view_search);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (groupFragment != null && groupFragment.isVisible()) {
            transaction.hide(groupFragment);
        }
        if (searchFragment == null) {
            transaction.add(R.id.activity_group_fragment_container_view_search, new SearchFragment(), SearchFragment.TAG);
        } else {
            transaction.show(searchFragment);
        }
        transaction.runOnCommit(() -> mSearchFragment = (SearchFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_group_fragment_container_view_search));
        transaction.commit();
    }

    private boolean onSearchCloseIconClick() {
        mSearchView.onActionViewCollapsed();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment searchFragment = fragmentManager.findFragmentById(R.id.activity_group_fragment_container_view_search);
        Fragment groupFragment = fragmentManager.findFragmentById(R.id.activity_group_fragment_container_view_group);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (searchFragment != null && searchFragment.isVisible()) {
            transaction.hide(searchFragment);
        }
        if (groupFragment == null) {
            transaction.add(R.id.activity_group_fragment_container_view_group, new GroupFragment(), GroupFragment.TAG);
        } else {
            transaction.show(groupFragment);
        }
        transaction.runOnCommit(() -> mGroupFragment = (GroupFragment) getSupportFragmentManager().
                findFragmentById(R.id.activity_group_fragment_container_view_group));
        transaction.commit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (android.R.id.home == itemId) {
            if (mSearchFragment != null && mSearchFragment.isVisible()) {
                onSearchCloseIconClick();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSearchResultSelect(String uid, String username, Drawable avatar) {
        onSearchCloseIconClick();
        mGroupFragment.onSearchResultSelect(uid, username, avatar);
    }

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_group_root_layout);
    }
}