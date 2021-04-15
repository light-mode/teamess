package com.example.myapplication.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.FragmentActivity;

import com.example.myapplication.R;
import com.example.myapplication.document.ChatsMessagesDocument;
import com.example.myapplication.document.UsersChatsDocument;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Utils {
    public static boolean isAuthenticateAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(Constants.ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isAuthenticateUsingBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int biometricAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        return biometricManager.canAuthenticate(biometricAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isAppLockRequired(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.pref_app_lock_key), false);
    }

    public static void simulateHomeButtonClick(@NonNull Activity activity) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        activity.startActivity(intent);
    }

    public static void showBackButton(@NonNull AppCompatActivity appCompatActivity) {
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    public static void hideBackButton(@NonNull AppCompatActivity appCompatActivity) {
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @NonNull
    public static CollectionReference getUsersRef() {
        return FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS);
    }

    @NonNull
    public static CollectionReference getUsersChatsRef(String uid) {
        return getUsersRef().document(uid).collection(Constants.COLLECTION_CHATS);
    }

    @NonNull
    public static CollectionReference getChatsRef() {
        return FirebaseFirestore.getInstance().collection(Constants.COLLECTION_CHATS);
    }

    @NonNull
    public static CollectionReference getChatsMessagesRef(String chatId) {
        return getChatsRef().document(chatId).collection(Constants.COLLECTION_MESSAGES);
    }

    @NonNull
    public static StorageReference getUsersAvatarRef(String uid) {
        return FirebaseStorage.getInstance().getReference().child("/" + Constants.COLLECTION_USERS + "/" + uid + "/" + "avatar.jpg");
    }

    @NonNull
    public static StorageReference getChatsFileRef(String chatId, String fileId) {
        return FirebaseStorage.getInstance().getReference().child("/" + Constants.COLLECTION_CHATS + "/" + chatId + "/" + fileId);
    }

    @NonNull
    public static StorageReference getChatsAvatarRef(String chatId) {
        return FirebaseStorage.getInstance().getReference().child("/" + Constants.COLLECTION_CHATS + "/" + chatId + "/" + "avatar.jpg");
    }

    public static void logTaskException(String tag, @NonNull Task<?> task) {
        Log.e(tag, task.getException() == null ? "" : task.getException().getMessage());
    }

    public static void updateCurrentUsersDocumentStatus(String status) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            getUsersRef().document(currentUser.getUid()).update(Constants.FIELD_STATUS, status);
        }
    }

    @NonNull
    public static String getCurrentUid() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException();
        }
        return currentUser.getUid();
    }

    public static void addSystemChatsMessagesDocument(String TAG, String chatId, String chatType, String content, @NonNull List<String> membersUid, Runnable callback) {
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        Timestamp now = Timestamp.now();
        String id = now.toString();
        String timestamp = Long.toString(now.getSeconds());

        ChatsMessagesDocument chatsMessagesDocument = new ChatsMessagesDocument();
        chatsMessagesDocument.setId(id);
        chatsMessagesDocument.setContent(content);
        chatsMessagesDocument.setTimestamp(timestamp);
        chatsMessagesDocument.setType(Constants.MESSAGE_TYPE_SYSTEM);
        batch.set(Utils.getChatsMessagesRef(chatId).document(id), chatsMessagesDocument);
        UsersChatsDocument usersChatsDocument = new UsersChatsDocument();
        usersChatsDocument.setId(chatId);
        usersChatsDocument.setType(chatType);
        usersChatsDocument.setLastMessageContent(content);
        usersChatsDocument.setLastMessageTimestamp(timestamp);
        usersChatsDocument.setLastMessageType(Constants.MESSAGE_TYPE_SYSTEM);
        String currentUid = getCurrentUid();
        for (String uid : membersUid) {
            if (Constants.CHAT_TYPE_SINGLE.equals(chatType)) {
                if (uid.equals(currentUid)) {
                    String otherUid = membersUid.stream().filter(o -> !o.equals(currentUid))
                            .collect(Collectors.toList()).get(0);
                    usersChatsDocument.setOtherUid(otherUid);
                } else {
                    usersChatsDocument.setOtherUid(currentUid);
                }
            }
            batch.set(Utils.getUsersChatsRef(uid).document(chatId), usersChatsDocument);
        }

        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                logTaskException(TAG, task);
                return;
            }
            if (callback != null) {
                callback.run();
            }
        });
    }

    public static void setLocale(@NonNull FragmentActivity activity) {
        String defaultLanguageCode = activity.getString(R.string.pref_language_code_english);
        String languageCode = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(activity.getString(R.string.pref_language_key), defaultLanguageCode);
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        activity.getBaseContext().getResources()
                .updateConfiguration(config, activity.getBaseContext().getResources().getDisplayMetrics());
    }

    public static Drawable getDefaultDrawable(Context context, int code) {
        int id = 0;
        switch (code) {
            case Constants.DEFAULT_PERSON_AVATAR_CODE:
                id = R.drawable.ic_baseline_person_24;
                break;
            case Constants.DEFAULT_GROUP_AVATAR_CODE:
                id = R.drawable.ic_baseline_group_24;
                break;
            case Constants.DEFAULT_IMAGE_CODE:
                id = R.drawable.ic_baseline_broken_image_24;
                break;
            default:
                break;
        }
        return AppCompatResources.getDrawable(context, id);
    }
}
