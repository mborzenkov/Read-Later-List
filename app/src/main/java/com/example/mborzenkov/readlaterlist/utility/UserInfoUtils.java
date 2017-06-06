package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.adt.UserInfo;

/** Вспомогательный static класс для получения работы с UserInfo.
 * Предоставляет работу с SharedPreferences, получени, изменение и сохранение текущего пользователя.
 */
public class UserInfoUtils {

    /** Ключ для доступа к данным о пользователях в SharedPreferences. */
    private static final String USERS_KEY = "com.example.mborzenkov.mainlist.users";
    /** Ключ для доступа к данным о последнем пользователе. */
    private static final String LAST_USER_KEY = "lastuser";
    /** Идентификатор пользователя по умолачнию. */
    private static final int DEFAULT_USER_ID = 0;

    /** Текущий пользователь. */
    private static UserInfo sCurrentUser = null;

    /** Возвращает текущего выбранного пользователя.
     *  Если пользователь еще не был установлен, читает его из SharedPreferences и устанавливает как currentUser.
     *  Если там нет записей, устанавливает пользователя по умолчанию.
     *
     * @param context контекст (для доступа к SharedPreferences)
     * @return текущий выбранный пользователь.
     */
    public static synchronized @NonNull UserInfo getCurentUser(@NonNull Context context) {
        if (sCurrentUser == null) {
            int lastUserId = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE)
                    .getInt(LAST_USER_KEY, DEFAULT_USER_ID);
            changeCurrentUser(context, lastUserId);
        }
        return sCurrentUser;
    }

    /** Меняет текущего пользователя на нового.
     *  Сохраняет нового пользователя как последнего в SharedPreferences.
     *
     * @param context контекст (для доступа к SharedPreferences)
     * @param newUserId идентификатор нового пользователя, >= 0
     *
     * @throws IllegalArgumentException если userId < 0
     * @throws IllegalArgumentException если длина userId > UserInfo.USER_ID_MAX_LENGTH
     *
     * @see UserInfo
     */
    public static synchronized void changeCurrentUser(@NonNull Context context, @IntRange(from = 0) int newUserId) {
        sCurrentUser = new UserInfo(newUserId);
        SharedPreferences.Editor editor = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE).edit();
        editor.putInt(LAST_USER_KEY, newUserId);
        editor.apply();
    }

    private UserInfoUtils() {
        throw new UnsupportedOperationException("Класс UserInfoUtils - static util, не может иметь экземпляров");
    }

}
