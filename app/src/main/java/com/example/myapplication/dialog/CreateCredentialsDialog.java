package com.example.myapplication.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;

public class CreateCredentialsDialog extends DialogFragment {
    public static final String TAG = "CreateCredentialsDialog";

    private Interface mImplementer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImplementer = (Interface) getActivity();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_create_credentials)
                .setNegativeButton(R.string.dialog_create_credentials_positive_button_text,
                        (dialog, which) -> mImplementer.onDialogReturn(TAG, Dialog.BUTTON_NEGATIVE))
                .setPositiveButton(R.string.dialog_create_credentials_negative_button_text,
                        (dialog, which) -> mImplementer.onDialogReturn(TAG, Dialog.BUTTON_POSITIVE))
                .create();
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        mImplementer.onDialogReturn(TAG, Dialog.BUTTON_NEUTRAL);
    }

    public interface Interface {
        void onDialogReturn(String tag, int dialogButtonValue);
    }
}
