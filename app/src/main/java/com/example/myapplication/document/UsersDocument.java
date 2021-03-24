package com.example.myapplication.document;

public class UsersDocument {
    private String username;
    private String usernameLowercase;
    private String dob;
    private String bio;
    private String status;

    public UsersDocument() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsernameLowercase() {
        return usernameLowercase;
    }

    public void setUsernameLowercase(String usernameLowercase) {
        this.usernameLowercase = usernameLowercase;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
