package com.example.myapplication.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.myapplication.R;
import com.example.myapplication.activity.SignInActivity;
import com.example.myapplication.activity.UserActivity;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.google.firebase.auth.FirebaseAuth;

public class MenuFragment extends Fragment {
    public static final String TAG = "MenuFragment";

    private FragmentActivity mActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null) {
            throw new RuntimeException();
        }
        mActivity = getActivity();
        // attachToRoot set to false is required
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button accountButton = view.findViewById(R.id.fragment_menu_button_account);
        accountButton.setOnClickListener(v -> onAccountButtonClick());
        Button logoutButton = view.findViewById(R.id.fragment_menu_button_logout);
        logoutButton.setOnClickListener(v -> onLogoutButtonClick());
    }

    private void onAccountButtonClick() {
        startActivity(new Intent(mActivity, UserActivity.class));
    }

    private void onLogoutButtonClick() {
        AlertDialog.Builder logoutDialog = new AlertDialog.Builder(getContext());
        logoutDialog.setTitle(getString(R.string.dialog_logout_title));
        logoutDialog.setPositiveButton(getString(R.string.dialog_logout_positive_button_text),
                this::onLogoutDialogPositiveButtonClick);
        logoutDialog.setNegativeButton(getString(R.string.dialog_logout_negative_button_text),
                null);
        logoutDialog.show();
    }

    private void onLogoutDialogPositiveButtonClick(DialogInterface dialog, int which) {
        Utils.updateUsersDocumentStatus(Constants.STATUS_OFFLINE);
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(mActivity, SignInActivity.class));
        mActivity.finish();
    }
}
