package com.example.myapplication.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.R;
import com.example.myapplication.utilities.Constants;
import com.google.android.material.tabs.TabLayout;

public class MainFragment extends Fragment {
    public static final String TAG = "MainFragment";

    private FragmentActivity mActivity;
    private TabLayout mTabLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // attachToRoot set to false is required
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mTabLayout = view.findViewById(R.id.fragment_main_tab_layout);
        setDefault();
        setOnClickListener();
        return view;
    }

    private void setDefault() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_main_fragment_container_view_home, new HomeFragment(), HomeFragment.TAG);
        transaction.commit();
    }

    private void setOnClickListener() {
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (Constants.TAB_POSITION_HOME == position) {
                    onHomeTabSelected();
                } else if (Constants.TAB_POSITION_CHAT == position) {
                    onChatTabSelected();
                } else if (Constants.TAB_POSITION_MENU == position) {
                    onMenuTabSelected();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void onHomeTabSelected() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment homeFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_home);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (homeFragment == null) {
            transaction.add(R.id.fragment_main_fragment_container_view_home, new HomeFragment(), HomeFragment.TAG);
        } else {
            transaction.show(homeFragment);
        }
        Fragment chatFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_chat);
        if (chatFragment != null && chatFragment.isVisible()) {
            transaction.hide(chatFragment);
        } else {
            Fragment menuFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_menu);
            if (menuFragment != null && menuFragment.isVisible()) {
                transaction.hide(menuFragment);
            }
        }
        transaction.commit();
    }

    private void onChatTabSelected() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment chatFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_chat);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (chatFragment == null) {
            transaction.add(R.id.fragment_main_fragment_container_view_chat, new ChatFragment(), ChatFragment.TAG);
        } else {
            transaction.show(chatFragment);
        }
        Fragment homeFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_home);
        if (homeFragment != null && homeFragment.isVisible()) {
            transaction.hide(homeFragment);
        } else {
            Fragment menuFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_menu);
            if (menuFragment != null && menuFragment.isVisible()) {
                transaction.hide(menuFragment);
            }
        }
        transaction.commit();
    }

    private void onMenuTabSelected() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment menuFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_menu);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (menuFragment == null) {
            transaction.add(R.id.fragment_main_fragment_container_view_menu, new MenuFragment(), MenuFragment.TAG);
        } else {
            transaction.show(menuFragment);
        }
        Fragment homeFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_home);
        if (homeFragment != null && homeFragment.isVisible()) {
            transaction.hide(homeFragment);
        } else {
            Fragment chatFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_chat);
            if (chatFragment != null && chatFragment.isVisible()) {
                transaction.hide(chatFragment);
            }
        }
        transaction.commit();
    }
}
