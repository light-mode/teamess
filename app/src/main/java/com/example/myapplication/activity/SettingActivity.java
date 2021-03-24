package com.example.myapplication.activity;

import android.os.Bundle;
import android.view.ViewGroup;

import com.example.myapplication.R;
import com.example.myapplication.fragment.SettingFragment;

public class SettingActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_setting_fragment_container_view, new SettingFragment())
                .commit();
    }

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_setting_root_layout);
    }
}