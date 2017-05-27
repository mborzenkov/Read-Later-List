package com.example.mborzenkov.readlaterlist.adt;

import android.support.annotation.IntRange;

/** Неизменяемый АДТ, прсдатвляющий пользователя.
 * Пользователь обладает идентификатором.
 */
public class UserInfo {

    /** Максимальная длина USER ID. */
    public static final int USER_ID_MAX_LENGTH = 8;

    /** Идентификатор пользователя. */
    private final @IntRange(from = 0) int userId;

    // Инвариант:
    //      userId - идентификатор пользователя, положительный и длиной не более USER_ID_MAX_LENGTH
    //
    // Абстрактная функция:
    //      представляет собой пользователя приложения, обладающего идентификатором
    //
    // Безопасность представления:
    //      все поля - неизменяемые объекты и объявлены final
    //
    // Потоковая безопасность:
    //      так как объект неизменяемый, он потокобезопасен

    /** Создает новый объект UserInfo с указанным идентификатором.
     *
     * @param userId идентификатор пользователя
     *
     * @throws IllegalArgumentException если userId < 0
     * @throws IllegalArgumentException если длина userId > USER_ID_MAX_LENGTH
     */
    public UserInfo(@IntRange(from = 0) int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("Error @ new UserInfo: userId == " + userId);
        } else if (String.valueOf(userId).length() > USER_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("Error @ new UserInfo: userId.length > USER_ID_MAX_LENGTH: " + userId);
        }
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
     * @return идентификатор пользователя, например "23234545"
     */
    @Override
    public String toString() {
        return String.valueOf(userId);
    }

}
