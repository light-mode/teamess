package com.example.myapplication.pojo;

import android.graphics.drawable.Drawable;

public class Member {
    private final String uid;
    private final String username;
    private final Drawable avatar;

    public Member(String uid, String username, Drawable avatar) {
        this.uid = uid;
        this.username = username;
        this.avatar = avatar;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public Drawable getAvatar() {
        return avatar;
    }
}
