package com.example.myapplication.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.R;
import com.example.myapplication.document.UsersDocument;
import com.example.myapplication.fragment.MainFragment;
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

public class MainActivity extends BaseActivity {
    public static final String TAG = "MainActivity";

    private SearchView mSearchView;
    private MainFragment mMainFragment;
    private SearchFragment mSearchFragment;

    private final String mCurrentUid;

    public MainActivity() {
        mCurrentUid = Utils.getCurrentUid();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setDefault();
    }

    private void setDefault() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.activity_main_fragment_container_view_main, new MainFragment(), MainFragment.TAG);
        transaction.runOnCommit(() -> mMainFragment = (MainFragment) getSupportFragmentManager().
                findFragmentById(R.id.activity_main_fragment_container_view_main));
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_main_item_search);
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
        Fragment mainFragment = fragmentManager.findFragmentById(R.id.activity_main_fragment_container_view_main);
        Fragment searchFragment = fragmentManager.findFragmentById(R.id.activity_main_fragment_container_view_search);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (mainFragment != null && mainFragment.isVisible()) {
            transaction.hide(mainFragment);
        }
        if (searchFragment == null) {
            transaction.add(R.id.activity_main_fragment_container_view_search, new SearchFragment(), SearchFragment.TAG);
        } else {
            transaction.show(searchFragment);
        }
        transaction.runOnCommit(() -> mSearchFragment = (SearchFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_main_fragment_container_view_search));
        transaction.commit();
        Utils.showBackButton(this);
    }

    private boolean onSearchCloseIconClick() {
        Utils.hideBackButton(this);
        mSearchView.onActionViewCollapsed();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment searchFragment = fragmentManager.findFragmentById(R.id.activity_main_fragment_container_view_search);
        Fragment mainFragment = fragmentManager.findFragmentById(R.id.activity_main_fragment_container_view_main);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (searchFragment != null && searchFragment.isVisible()) {
            transaction.hide(searchFragment);
        }
        if (mainFragment == null) {
            transaction.add(R.id.activity_main_fragment_container_view_main, new MainFragment(), MainFragment.TAG);
        } else {
            transaction.show(mainFragment);
        }
        transaction.runOnCommit(() -> mMainFragment = (MainFragment) getSupportFragmentManager().
                findFragmentById(R.id.activity_main_fragment_container_view_main));
        transaction.commit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (android.R.id.home == itemId) {
            onSearchCloseIconClick();
            return true;
        } else if (R.id.menu_main_item_create_group == itemId) {
            Intent intent = new Intent(this, GroupActivity.class);
            intent.putExtra(Constants.EXTRA_MODE, Constants.MODE_CREATE_GROUP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment searchFragment = getSupportFragmentManager().findFragmentById(R.id.activity_main_fragment_container_view_search);
        if (searchFragment != null && searchFragment.isVisible()) {
            onSearchCloseIconClick();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_main_root_layout);
    }
}