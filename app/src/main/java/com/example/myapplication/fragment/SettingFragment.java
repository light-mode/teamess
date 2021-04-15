package com.example.myapplication.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.example.myapplication.R;
import com.example.myapplication.activity.SettingActivity;

import java.util.Arrays;
import java.util.List;

public class SettingFragment extends PreferenceFragmentCompat {
    public SwitchPreferenceCompat appLockPref;

    private SettingActivity mActivity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        FragmentActivity activity = getActivity();
        if (context == null || activity == null) {
            return;
        }
        if (activity instanceof SettingActivity) {
            mActivity = (SettingActivity) activity;
        } else {
            return;
        }
        setPreferencesFromResource(R.xml.preferences, rootKey);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadAppLockPref(sharedPreferences);
        loadLanguagePref(sharedPreferences, context);
        loadHourFormatPref(sharedPreferences, context);
    }

    private void loadAppLockPref(@NonNull SharedPreferences sharedPreferences) {
        boolean appLockRequired = sharedPreferences.getBoolean(getString(R.string.pref_app_lock_key), false);
        appLockPref = getPreferenceManager().findPreference(getString(R.string.pref_app_lock_key));
        if (appLockPref == null) {
            return;
        }
        appLockPref.setChecked(appLockRequired);
        appLockPref.setOnPreferenceChangeListener((preference, newValue) -> {
            mActivity.authenticated = true;
            mActivity.authenticate();
            return true;
        });
    }

    private void loadLanguagePref(@NonNull SharedPreferences sharedPreferences, Context context) {
        String defaultLanguageCode = getString(R.string.pref_language_code_english);
        String languageCode = sharedPreferences.getString(getString(R.string.pref_language_key), defaultLanguageCode);
        List<String> languageCodes = Arrays.asList(getResources().getStringArray(R.array.language_codes));
        ListPreference languagePref = getPreferenceManager().findPreference(getString(R.string.pref_language_key));
        if (languagePref == null) {
            return;
        }
        languagePref.setValueIndex(languageCodes.indexOf(languageCode));
        languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof String)) {
                return false;
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(getString(R.string.pref_language_key), newValue.toString());
            editor.apply();
            return true;
        });
    }

    private void loadHourFormatPref(@NonNull SharedPreferences sharedPreferences, Context context) {
        boolean hourFormat = sharedPreferences.getBoolean(getString(R.string.pref_24_hour_format_key), false);
        SwitchPreferenceCompat hourFormatPref = getPreferenceManager().findPreference(getString(R.string.pref_24_hour_format_key));
        if (hourFormatPref == null) {
            return;
        }
        hourFormatPref.setChecked(hourFormat);
        hourFormatPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof Boolean)) {
                return false;
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(getString(R.string.pref_24_hour_format_key), (Boolean) newValue);
            editor.apply();
            return true;
        });
    }
}
