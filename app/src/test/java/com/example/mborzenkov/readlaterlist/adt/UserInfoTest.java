package com.example.mborzenkov.readlaterlist.adt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Тестирует UserInfo. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class UserInfoTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    @Test
    public void testUserInfo() {
        final int userId = 1;
        UserInfo userInfo = new UserInfo(userId);
        assertEquals(userInfo.getUserId(), userId);
    }

    @Test
    public void testEquals() {
        UserInfo userInfo1 = new UserInfo(1);
        UserInfo userInfo2 = new UserInfo(1);
        assertEquals(userInfo1, userInfo2);
        assertEquals(userInfo1.getUserId(), userInfo2.getUserId());
        assertEquals(userInfo1.hashCode(), userInfo2.hashCode());
        userInfo2 = new UserInfo(0);
        assertFalse(userInfo1.equals(userInfo2));
        assertFalse(userInfo1.equals(new Object()));
    }

    @Test
    public void testToString() {
        final int userId = 12345678;
        UserInfo userInfo = new UserInfo(userId);
        assertTrue(userInfo.toString().contains(String.valueOf(userId)));
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testMaxLengthError() {
        final int userId = 123456789;
        UserInfo userInfo = new UserInfo(userId);
        // throws exception
        assertEquals(null, userInfo);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIdError() {
        //noinspection Range
        UserInfo userInfo = new UserInfo(-1);
        // throws exception
        assertEquals(null, userInfo);
    }

}
