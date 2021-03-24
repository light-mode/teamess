package com.example.myapplication.pojo;

import com.google.firebase.storage.StorageReference;

public class SearchResult {
    private final String uid;
    private final String bio;
    private final String username;
    private final StorageReference avatarRef;

    public SearchResult(String uid, String bio, String username, StorageReference avatarRef) {
        this.uid = uid;
        this.bio = bio;
        this.username = username;
        this.avatarRef = avatarRef;
    }

    public String getUid() {
        return uid;
    }

    public String getBio() {
        return bio;
    }

    public StorageReference getAvatarRef() {
        return avatarRef;
    }

    public String getUsername() {
        return username;
    }
}
