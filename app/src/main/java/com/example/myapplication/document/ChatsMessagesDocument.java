package com.example.myapplication.document;

import java.util.List;

public class ChatsMessagesDocument {
    private String id;
    private String type;
    private String content;
    private String timestamp;
    private String senderUid;
    private String downloadUrl;
    private List<String> seenUserIds;
    private boolean deleted;

    public ChatsMessagesDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public List<String> getSeenUserIds() {
        return seenUserIds;
    }

    public void setSeenUserIds(List<String> seenUserIds) {
        this.seenUserIds = seenUserIds;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
