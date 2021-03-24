package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
        } else {
            Utils.getUsersRef().document(currentUser.getUid()).get()
                    .addOnCompleteListener(this::onGetUsersDocumentComplete);
        }
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
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}