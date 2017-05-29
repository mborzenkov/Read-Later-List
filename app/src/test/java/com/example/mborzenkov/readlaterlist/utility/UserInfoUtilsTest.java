package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mborzenkov.readlaterlist.adt.UserInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/** Тестирует UserInfoUtils. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class UserInfoUtilsTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int DEFAULT_USER_ID = 1005930;
    private static final int SECOND_USER_ID = 1;
    private static final int MAX_ID = 1234567890;

    private Context mContext;

    @Before
    public void prepareMock() {
        mContext = Mockito.mock(Context.class);
        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final SharedPreferences.Editor sharedPrefsEditor = Mockito.mock(SharedPreferences.Editor.class);
        Mockito.when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);
        Mockito.when(sharedPrefs.getInt(anyString(), anyInt())).thenReturn(DEFAULT_USER_ID);
        Mockito.when(sharedPrefs.edit()).thenReturn(sharedPrefsEditor);
        Mockito.when(sharedPrefsEditor.putInt(anyString(), anyInt())).thenReturn(sharedPrefsEditor);
    }

    @Test
    public void testGetCurrentUser() {
        final UserInfo currentUser = UserInfoUtils.getCurentUser(mContext);
        assertEquals(DEFAULT_USER_ID, currentUser.getUserId());
    }

    @Test
    public void testChangeCurrentUser() {
        UserInfoUtils.getCurentUser(mContext);
        UserInfoUtils.changeCurrentUser(mContext, SECOND_USER_ID);
        assertEquals(SECOND_USER_ID, UserInfoUtils.getCurentUser(mContext).getUserId());
        UserInfoUtils.changeCurrentUser(mContext, DEFAULT_USER_ID);
        assertEquals(DEFAULT_USER_ID, UserInfoUtils.getCurentUser(mContext).getUserId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIdException() {
        //noinspection Range
        UserInfoUtils.changeCurrentUser(mContext, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxIdException() {
        UserInfoUtils.changeCurrentUser(mContext, MAX_ID);
    }

}
