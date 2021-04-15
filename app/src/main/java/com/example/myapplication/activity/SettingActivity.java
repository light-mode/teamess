package com.example.myapplication.activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.example.myapplication.R;
import com.example.myapplication.dialog.CreateCredentialsDialog;
import com.example.myapplication.fragment.SettingFragment;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;

import java.util.concurrent.Executor;

public class SettingActivity extends AppCompatActivity implements CreateCredentialsDialog.Interface {
    public boolean authenticated;

    private ViewGroup mRootLayout;
    private SettingFragment mSettingFragment;
    private ActivityResultLauncher<Intent> mSecuritySettingsLauncher;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (getString(R.string.pref_language_key).equals(key)) {
            getIntent().putExtra(Constants.EXTRA_AUTHENTICATED, true);
            recreate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(listener);
        Utils.setLocale(this);
        setContentView(R.layout.activity_setting);
        setTitle(getString(R.string.activity_setting_title));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        mRootLayout = findViewById(R.id.activity_setting_root_layout);
        mSettingFragment = new SettingFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_setting_fragment_container_view, mSettingFragment)
                .commit();
        mSecuritySettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Utils.isAuthenticateAvailable(this)) {
                promptUserToLogin();
            } else {
                rollbackAppLockPref();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.getBooleanExtra(Constants.EXTRA_AUTHENTICATED, false)) {
            authenticated = true;
            intent.removeExtra(Constants.EXTRA_AUTHENTICATED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticated();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRootLayout.setVisibility(View.INVISIBLE);
        Utils.updateCurrentUsersDocumentStatus(Constants.STATUS_OFFLINE);
        authenticated = false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (android.R.id.home == itemId) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAuthenticated() {
        boolean authenticated = this.authenticated && Utils.isAuthenticateAvailable(this);
        boolean appLockRequired = Utils.isAppLockRequired(this);
        if (!authenticated && appLockRequired) {
            mRootLayout.setVisibility(View.INVISIBLE);
            authenticate();
        } else {
            mRootLayout.setVisibility(View.VISIBLE);
            Utils.updateCurrentUsersDocumentStatus(Constants.STATUS_ONLINE);
        }
    }

    public void authenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(Constants.ALLOWED_AUTHENTICATORS)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                promptUserToLogin();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                showCreateCredentialsDialog();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            default:
                break;
        }
    }

    private void promptUserToLogin() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (authenticated) {
                    rollbackAppLockPref();
                } else {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        Utils.simulateHomeButtonClick(SettingActivity.this);
                    }
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                mRootLayout.setVisibility(View.VISIBLE);
                authenticated = true;
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.pref_app_lock_title))
                .setSubtitle(Utils.isAuthenticateUsingBiometricAvailable(this)
                        ? getString(R.string.biometric_prompt_subtitle_fingerprint)
                        : getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(Constants.ALLOWED_AUTHENTICATORS)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void showCreateCredentialsDialog() {
        Fragment previous = getSupportFragmentManager().findFragmentByTag(CreateCredentialsDialog.TAG);
        if (previous != null) {
            ((CreateCredentialsDialog) previous).dismiss();
        }
        new CreateCredentialsDialog().show(getSupportFragmentManager(), CreateCredentialsDialog.TAG);
    }

    @Override
    public void onDialogReturn(String tag, int dialogButtonValue) {
        if (CreateCredentialsDialog.TAG.equals(tag)) {
            switch (dialogButtonValue) {
                case Dialog.BUTTON_POSITIVE:
                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    mSecuritySettingsLauncher.launch(intent);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                case Dialog.BUTTON_NEUTRAL:
                default:
                    rollbackAppLockPref();
                    break;
            }
        }
    }

    private void rollbackAppLockPref() {
        authenticated = false;
        boolean isAppLockRequired = Utils.isAppLockRequired(this);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(getString(R.string.pref_app_lock_key), !isAppLockRequired);
        editor.apply();
        mSettingFragment.appLockPref.setChecked(!isAppLockRequired);
    }
}