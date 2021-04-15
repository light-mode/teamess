package com.example.myapplication.utilities;

import androidx.biometric.BiometricManager;

public class Constants {
    public static final int ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
            | BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CHATS = "chats";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_SEEN_USER_IDS = "seenUserIds";

    public static final String FIELD_STATUS = "status";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_USERNAME_LOWERCASE = "usernameLowercase";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_MEMBERS_UID = "membersUid";
    public static final String FIELD_DELETED = "deleted";
    public static final String FIELD_CREATOR_UID = "creatorUid";
    public static final String FIELD_BLOCKER_UID = "blockerUid";
    public static final String FIELD_AVATAR_TIMESTAMP = "avatarTimestamp";
    public static final String FIELD_LAST_MESSAGE_DELETED = "lastMessageDeleted";

    public static final String MODE_VIEW_GROUP = "MODE_VIEW_GROUP";
    public static final String MODE_EDIT_GROUP = "MODE_EDIT_GROUP";
    public static final String MODE_ADD_MEMBER = "MODE_ADD_MEMBER";
    public static final String MODE_CREATE_GROUP = "MODE_CREATE_GROUP";
    public static final String MODE_REMOVE_MEMBER = "MODE_REMOVE_MEMBER";

    public static final String KEY_MODE = "KEY_MODE";
    public static final String KEY_CHAT_ID = "KEY_CHAT_ID";
    public static final String KEY_OTHERS_USERNAME = "KEY_OTHERS_USERNAME";

    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_IMAGE = "EXTRA_IMAGE";
    public static final String EXTRA_CHAT_ID = "EXTRA_CHAT_ID";
    public static final String EXTRA_IMAGE_ID = "EXTRA_IMAGE_ID";
    public static final String EXTRA_CHAT_TYPE = "EXTRA_CHAT_TYPE";
    public static final String EXTRA_OTHER_UID = "EXTRA_OTHER_UID";
    public static final String EXTRA_AUTHENTICATED = "EXTRA_AUTHENTICATED";
    public static final String EXTRA_OTHERS_USERNAME = "EXTRA_OTHERS_USERNAME";
    public static final String EXTRA_USERNAME_NOT_SET = "EXTRA_USERNAME_NOT_SET";

    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";

    public static final String CHAT_TYPE_SINGLE = "single";
    public static final String CHAT_TYPE_GROUP = "group";

    public static final int TAB_POSITION_CHAT = 0;
    public static final int TAB_POSITION_MENU = 1;

    public static final int NAME_LENGTH_MIN = 5;
    public static final int NAME_LENGTH_MAX = 50;
    public static final long FILE_UPLOAD_LIMIT_IN_BYTE = 15 * 1024 * 1024;

    public static final int REQUEST_CAMERA_PERMISSION_CODE = 0;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 1;

    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_ICON = "icon";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_FILE = "file";
    public static final String MESSAGE_TYPE_SYSTEM = "system";

    public static final String USER_ADDED = "_ADD_USER_";
    public static final String USER_REMOVED = "_REMOVE_USER_";
    public static final String GROUP_CREATED = "_CREATE_GROUP_";
    public static final String GROUP_NAME_CHANGED = "_CHANGE_GROUP_NAME_TO_";
    public static final String GROUP_AVATAR_CHANGED = "_CHANGE_GROUP_AVATAR";
    public static final String BLOCKED = "_BLOCKED_";
    public static final String UNBLOCKED = "_UNBLOCKED_";

    public static final int ICON_VERY_DISSATISFIED = 0;
    public static final int ICON_DISSATISFIED = 1;
    public static final int ICON_NEUTRAL = 2;
    public static final int ICON_SATISFIED = 3;
    public static final int ICON_SATISFIED_ALT = 4;
    public static final int ICON_VERY_SATISFIED = 5;

    public static final String DOB_SEPARATOR = "-";

    public static final int DEFAULT_PERSON_AVATAR_CODE = 0;
    public static final int DEFAULT_GROUP_AVATAR_CODE = 1;
    public static final int DEFAULT_IMAGE_CODE = 2;
}
