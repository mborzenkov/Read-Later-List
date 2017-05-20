package com.example.mborzenkov.readlaterlist.adt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/** Объект, представляющий информацию о пользователе. */
public class UserInfo {

    /** Ключ для доступа к данным о пользователях в SharedPreferences. */
    private static final String USERS_KEY = "com.example.mborzenkov.mainlist.users";
    /** Ключ для доступа к данным о последнем пользователе. */
    private static final String LAST_USER_KEY = "lastuser";
    /** Идентификатор пользователя по умолачнию. */
    private static final int DEFAULT_USER_ID = 0;

    /** Текущий пользователь. */
    private static UserInfo curentUser = null;

    // --Commented out by Inspection START (20.05.17, 21:49):
    //    /** Проверяет, был ли уже установлен текущий пользователь.
    //     *  Текущий пользователь устанавливается при первом доступе к getCurrentUser, по этому если он не установлен,
    //     *      то это почти наверняка запуск приложения.
    //     *
    //     * @return true - если был, иначе false
    //     */
    //    public static boolean userInfoNotSet() {
    //        return curentUser == null;
    //    }
    // --Commented out by Inspection STOP (20.05.17, 21:49)

    /** Возвращает текущего выбранного пользователя.
     *  Если пользователь еще не был установлен, читает его из SharedPreferences и устанавливает как currentUser.
     *  Если там нет записей, устанавливает пользователя по умолчанию.
     *
     * @param context контекст (для доступа к SharedPreferences)
     * @return текущий выбранный пользователь.
     */
    public static synchronized @NonNull UserInfo getCurentUser(@NonNull Context context) {
        if (curentUser == null) {
            int lastUserId = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE)
                    .getInt(LAST_USER_KEY, DEFAULT_USER_ID);
            changeCurrentUser(context, lastUserId);
        }
        return curentUser;
    }

    /** Меняет текущего пользователя на нового.
     *  Сохраняет нового пользователя как последнего в SharedPreferences.
     *
     * @param context контекст (для доступа к SharedPreferences)
     * @param newUserId идентификатор нового пользователя
     */
    public static synchronized void changeCurrentUser(@NonNull Context context, int newUserId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE).edit();
        editor.putInt(LAST_USER_KEY, newUserId);
        editor.apply();
        curentUser = new UserInfo(newUserId);
    }


    // -- Объект

    // Идентификатор пользователя
    private final int userId;

    /** Создает новый объект UserInfo с указанным идентификатором.
     *
     * @param userId идентификатор пользователя
     */
    private UserInfo(int userId) {
        this.userId = userId;
    }

    /** Возвращает идентификатор пользователя.
     *
     * @return идентификатор пользователя
     */
    public int getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        int result = 17;
        return 31 * result + userId;
    }

    /** Два пользователя равны, если их идентификаторы равны.
     *
     * @param obj Объект для сравнения
     * @return true - если пользователи равны, false иначе
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserInfo)) {
            return false;
        }
        UserInfo thatObject = (UserInfo) obj;
        return userId == thatObject.userId;
    }

    /** Возвращает строковое представление идентификатора пользователя.
     *
     * @return идентификатор пользователя, например "2323"
     */
    @Override
    public String toString() {
        return String.valueOf(userId);
    }

}
