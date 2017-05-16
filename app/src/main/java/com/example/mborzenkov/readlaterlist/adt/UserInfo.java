package com.example.mborzenkov.readlaterlist.adt;

public class UserInfo {

    private static UserInfo curentUser = null;

    public static UserInfo getCurentUser() {
        if (curentUser == null) {
            int userId = 1005931;
            curentUser = new UserInfo(userId);
        }
        return curentUser;
    }

    private final int userId;

    private UserInfo(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
