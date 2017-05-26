package com.example.mborzenkov.readlaterlist.adt;

/** Неизменяемый АДТ, прсдатвляющий пользователя.
 * Пользователь обладает идентификатором.
 */
public class UserInfo {

    /** Максимальная длина USER ID. */
    public static final int USER_ID_MAX_LENGTH = 8;

    /** Идентификатор пользователя. */
    private final int userId;

    // Инвариант:
    //      userId - идентификатор пользователя
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
     */
    public UserInfo(int userId) {
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
