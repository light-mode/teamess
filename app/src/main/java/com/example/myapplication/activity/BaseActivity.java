package com.example.myapplication.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.myapplication.dialog.CreateCredentialsDialog;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;

import java.util.concurrent.Executor;

public abstract class BaseActivity extends AppCompatActivity implements CreateCredentialsDialog.Interface {
    private static boolean sAuthenticated;
    private ActivityResultLauncher<Intent> mSecuritySettingsLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSecuritySettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Utils.isAuthenticateAvailable(getApplicationContext())) {
                promptUserToLogin();
            } else {
                showCreateCredentialsDialog();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.getBooleanExtra(Constants.EXTRA_AUTHENTICATED, false)) {
            sAuthenticated = true;
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
        getCurrentRootLayout().setVisibility(View.INVISIBLE);
        Utils.updateUsersDocumentStatus(Constants.STATUS_OFFLINE);
        sAuthenticated = false;
    }

    private void checkAuthenticated() {
        boolean authenticated = sAuthenticated && Utils.isAuthenticateAvailable(this);
        boolean passwordRequired = Utils.isPasswordRequired(this);
        if (!authenticated && passwordRequired) {
            getCurrentRootLayout().setVisibility(View.INVISIBLE);
            authenticate();
        } else {
            getCurrentRootLayout().setVisibility(View.VISIBLE);
            Utils.updateUsersDocumentStatus(Constants.STATUS_ONLINE);
        }
    }

    private void authenticate() {
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
                if (getCurrentActivity().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    Utils.simulateHomeButtonClick(getCurrentActivity());
                }
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                getCurrentRootLayout().setVisibility(View.VISIBLE);
                sAuthenticated = true;
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("App Lock")
                .setSubtitle(Utils.isAuthenticateUsingBiometricAvailable(this)
                        ? "Enter your password or confirm fingerprint to continue"
                        : "Enter your password to continue")
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
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mSecuritySettingsLauncher.launch(intent);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                case Dialog.BUTTON_NEUTRAL:
                default:
                    Utils.simulateHomeButtonClick(getCurrentActivity());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sAuthenticated = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        sAuthenticated = true;
    }

    public abstract BaseActivity getCurrentActivity();

    public abstract ViewGroup getCurrentRootLayout();
}
