package com.example.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.myapplication.R;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class EntryActivity extends AppCompatActivity {
    public static final String TAG = "EntryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(EntryActivity.this, SignInActivity.class));
            finish();
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean usernameSet = sharedPreferences.getBoolean(getString(R.string.pref_username_set), false);
        if (usernameSet) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        Utils.getUsersRef().document(currentUser.getUid()).get()
                .addOnCompleteListener(this::onGetUsersDocumentComplete);
    }

    private void onGetUsersDocumentComplete(@NonNull Task<DocumentSnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) {
            Utils.logTaskException(TAG, task);
            return;
        }
        Intent intent;
        if (task.getResult().getData() == null) {
            intent = new Intent(this, UserActivity.class);
            intent.putExtra(Constants.EXTRA_USERNAME_NOT_SET, true);
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(getString(R.string.pref_username_set), true);
            editor.apply();
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}