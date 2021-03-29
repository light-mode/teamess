package com.example.myapplication.document;

import java.util.List;

public class ChatsDocument {
    private String type;
    private String name;
    private String creatorUid;
    private String blockerUid;
    private String avatarTimestamp;
    private List<String> membersUid;

    public ChatsDocument() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public String getBlockerUid() {
        return blockerUid;
    }

    public void setBlockerUid(String blockerUid) {
        this.blockerUid = blockerUid;
    }

    public String getAvatarTimestamp() {
        return avatarTimestamp;
    }

    public void setAvatarTimestamp(String avatarTimestamp) {
        this.avatarTimestamp = avatarTimestamp;
    }

    public List<String> getMembersUid() {
        return membersUid;
    }

    public void setMembersUid(List<String> membersUid) {
        this.membersUid = membersUid;
    }
}
