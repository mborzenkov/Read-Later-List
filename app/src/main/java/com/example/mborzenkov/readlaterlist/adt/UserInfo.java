package com.example.mborzenkov.readlaterlist.adt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserInfo {

    private static final String USERS_KEY = "com.example.mborzenkov.mainlist.users";
    private static final String LAST_USER_KEY = "lastuser";
    private static final int DEFAULT_USER_ID = 0;

    private static UserInfo curentUser = null;

    public static boolean userInfoNotSet() {
        return curentUser == null;
    }

    public static synchronized @NonNull UserInfo getCurentUser(@NonNull Context context) {
        if (curentUser == null) {
            int lastUserId = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE)
                    .getInt(LAST_USER_KEY, DEFAULT_USER_ID);
            changeCurrentUser(context, Integer.valueOf(lastUserId));
        }
        return curentUser;
    }

    public static synchronized void changeCurrentUser(@NonNull Context context, int newUserId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE).edit();
        editor.putInt(LAST_USER_KEY, newUserId);
        editor.apply();
        curentUser = new UserInfo(newUserId);
    }

    private final int userId;

    private UserInfo(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        int result = 17;
        return 31 * result + userId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserInfo)) {
            return false;
        }
        UserInfo thatObject = (UserInfo) obj;
        return userId == thatObject.userId;
    }

    @Override
    public String toString() {
        return String.valueOf(userId);
    }

}
