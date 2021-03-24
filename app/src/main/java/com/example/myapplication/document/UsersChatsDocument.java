package com.example.myapplication.document;

public class UsersChatsDocument {
    private String id;
    private String type;
    private String otherUid;
    private String lastMessageType;
    private String lastMessageContent;
    private String lastMessageTimestamp;
    private String lastMessageSenderUid;
    private boolean lastMessageDeleted;

    public UsersChatsDocument() {
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

    public String getOtherUid() {
        return otherUid;
    }

    public void setOtherUid(String otherUid) {
        this.otherUid = otherUid;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public String getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderUid() {
        return lastMessageSenderUid;
    }

    public void setLastMessageSenderUid(String lastMessageSenderUid) {
        this.lastMessageSenderUid = lastMessageSenderUid;
    }

    public boolean isLastMessageDeleted() {
        return lastMessageDeleted;
    }

    public void setLastMessageDeleted(boolean lastMessageDeleted) {
        this.lastMessageDeleted = lastMessageDeleted;
    }
}
