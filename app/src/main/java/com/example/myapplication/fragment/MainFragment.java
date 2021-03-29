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

import java.util.Objects;

public class MainFragment extends Fragment {
    public static final String TAG = "MainFragment";

    private TabLayout mTabLayout;
    private FragmentActivity mActivity;

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
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case Constants.TAB_POSITION_CHAT:
                        onChatTabSelected();
                        break;
                    case Constants.TAB_POSITION_MENU:
                        onMenuTabSelected();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        onChatTabSelected();
        return view;
    }

    private void onChatTabSelected() {
        Objects.requireNonNull(mTabLayout.getTabAt(Constants.TAB_POSITION_CHAT)).select();
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment menuFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_menu);
        Fragment chatFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_chat);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (menuFragment != null) {
            transaction.hide(menuFragment);
        }
        if (chatFragment == null) {
            transaction.add(R.id.fragment_main_fragment_container_view_chat, new ChatFragment(), ChatFragment.TAG);
        } else {
            transaction.show(chatFragment);
        }
        transaction.commit();
    }

    private void onMenuTabSelected() {
        Objects.requireNonNull(mTabLayout.getTabAt(Constants.TAB_POSITION_MENU)).select();
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment chatFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_chat);
        Fragment menuFragment = fragmentManager.findFragmentById(R.id.fragment_main_fragment_container_view_menu);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (chatFragment != null) {
            transaction.hide(chatFragment);
        }
        if (menuFragment == null) {
            transaction.add(R.id.fragment_main_fragment_container_view_menu, new MenuFragment(), MenuFragment.TAG);
        } else {
            transaction.show(menuFragment);
        }
        transaction.commit();
    }
}
