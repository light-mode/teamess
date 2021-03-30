package com.example.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.LanguageAdapter;
import com.example.myapplication.pojo.Language;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignInActivity extends AppCompatActivity implements LanguageAdapter.OnItemClickListener {
    public static final String TAG = "SignInActivity";

    private Button mContinueButton;

    private ActivityResultLauncher<Intent> mSignInLauncher;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (getString(R.string.pref_language_key).equals(key)) {
            recreate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(listener);
        Utils.setLocale(this);
        setTitle(R.string.activity_sign_in_title);
        setContentView(R.layout.activity_sign_in);

        List<Language> languages = new ArrayList<>();
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String[] languageNames = getResources().getStringArray(R.array.language_names);
        for (int index = 0; index < languageCodes.length; index++) {
            Language language = new Language(languageCodes[index], languageNames[index]);
            languages.add(language);
        }

        RecyclerView mRecyclerView = findViewById(R.id.activity_sign_in_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new LanguageAdapter(this, languages, this));
        mContinueButton = findViewById(R.id.activity_sign_in_button_continue);
        mContinueButton.setOnClickListener(v -> onContinueButtonClick());
        mSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onSignInIntentReturn);
    }

    @Override
    public void onItemClick(String code) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.pref_language_key), code);
        editor.apply();
    }

    private void onContinueButtonClick() {
        mContinueButton.setEnabled(false);
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAlwaysShowSignInMethodScreen(true)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build();
        mSignInLauncher.launch(intent);
    }

    private void onSignInIntentReturn(@NonNull ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }
            Utils.getUsersRef().document(currentUser.getUid()).get()
                    .addOnCompleteListener(this::onGetUsersDocumentComplete);
        } else {
            mContinueButton.setEnabled(true);
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