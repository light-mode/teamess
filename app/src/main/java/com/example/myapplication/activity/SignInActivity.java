package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;
import java.util.List;

public class SignInActivity extends AppCompatActivity {
    public static final String TAG = "SignInActivity";

    private Button mSignInButton;
    private ActivityResultLauncher<Intent> mSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setTitle(getString(R.string.activity_sign_in_title));
        mSignInButton = findViewById(R.id.activity_sign_in_button_sign_in);
        mSignInButton.setOnClickListener(v -> onSignInButtonClick());
        mSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onSignInIntentReturn);
    }

    private void onSignInButtonClick() {
        mSignInButton.setVisibility(View.INVISIBLE);
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAlwaysShowSignInMethodScreen(true)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build();
        mSignInLauncher.launch(intent);
    }

    private void onSignInIntentReturn(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }
            Utils.getUsersRef().document(currentUser.getUid()).get()
                    .addOnCompleteListener(this::onGetUsersDocumentComplete);
        } else {
            mSignInButton.setVisibility(View.VISIBLE);
        }
    }

    private void onGetUsersDocumentComplete(Task<DocumentSnapshot> task) {
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