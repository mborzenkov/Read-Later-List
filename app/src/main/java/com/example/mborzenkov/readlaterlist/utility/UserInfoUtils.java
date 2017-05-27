package com.example.mborzenkov.readlaterlist.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.example.mborzenkov.readlaterlist.R;
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
     * @param newUserId идентификатор нового пользователя
     */
    public static synchronized void changeCurrentUser(@NonNull Context context, int newUserId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USERS_KEY, Context.MODE_PRIVATE).edit();
        editor.putInt(LAST_USER_KEY, newUserId);
        editor.apply();
        sCurrentUser = new UserInfo(newUserId);
    }

    /** Показывает диалог смены пользователя и меняет текущего пользователя, если был выбран другой пользователь.
     * Если был выбран новый пользователь, не равный текущему, меняет текущего на новый и вызывает afterChangeAction.
     *
     * @param activity активити для создания поля ввода
     * @param afterChangeAction действие после смены пользоваля или null, если действие не нужно
     */
    public static void showDialogAndChangeUser(@NonNull final Activity activity, @Nullable final Runnable afterChangeAction) {

        if (sCurrentUser == null) {
            return;
        }

        final int currentUserId = sCurrentUser.getUserId();
        EditText inputNumber = new EditText(activity);
        inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(UserInfo.USER_ID_MAX_LENGTH)});
        inputNumber.setText(String.valueOf(currentUserId));
        ActivityUtils.showInputTextDialog(
                activity,
                inputNumber,
                activity.getString(R.string.mainlist_user_change_question_title),
                activity.getString(R.string.mainlist_user_change_question_text),
                new ActivityUtils.Consumer<String>() {
                    @Override
                    public void accept(final String param) {
                        try {
                            // Смотрим введенное значение
                            int number = Integer.parseInt(param);
                            if (number != currentUserId) {
                                UserInfoUtils.changeCurrentUser(activity, number);
                                if (afterChangeAction != null) {
                                    afterChangeAction.run();
                                }
                            }
                        } catch (ClassCastException e) {
                            Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                        }
                    }
                },
                null);

    }

    private UserInfoUtils() {
        throw new UnsupportedOperationException("Класс UserInfoUtils - static util, не может иметь экземпляров");
    }

}
