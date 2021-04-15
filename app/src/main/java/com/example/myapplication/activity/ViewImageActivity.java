package com.example.myapplication.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;
import com.example.myapplication.utilities.Constants;
import com.example.myapplication.utilities.Utils;

public class ViewImageActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);
        setTitle(getIntent().getStringExtra(Constants.EXTRA_IMAGE_ID));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new RuntimeException();
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        byte[] image = getIntent().getByteArrayExtra(Constants.EXTRA_IMAGE);
        ImageView mContentImageView = findViewById(R.id.activity_view_image_image_view_content);
        Glide.with(this).load(image)
                .error(Utils.getDefaultDrawable(this, Constants.DEFAULT_IMAGE_CODE))
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mContentImageView);
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

    @Override
    public BaseActivity getCurrentActivity() {
        return this;
    }

    @Override
    public ViewGroup getCurrentRootLayout() {
        return findViewById(R.id.activity_view_image_root_layout);
    }
}