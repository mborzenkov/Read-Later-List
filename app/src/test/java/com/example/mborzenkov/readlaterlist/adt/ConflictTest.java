package com.example.mborzenkov.readlaterlist.adt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Тестирует Conflict. */
@SuppressWarnings("FieldCanBeLocal") // Поля вынесены на уровень класса для улучшенной читаемости
public class ConflictTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/tests/testDebugUnitTest/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/html/index.html

    @Test
    public void testConflict() {
        ReadLaterItem item1 = new ReadLaterItem.Builder("label").remoteId(1).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder("label2").remoteId(1).build();
        Conflict conflict = new Conflict(item1, item2);
        assertEquals(item1, conflict.getLeft());
        assertEquals(item2, conflict.getRight());
    }

    @Test
    public void testEquals() {
        ReadLaterItem item1 = new ReadLaterItem.Builder("label").remoteId(1).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder("label2").remoteId(1).build();
        Conflict conflict1 = new Conflict(item1, item2);
        assertEquals(item1, conflict1.getLeft());
        assertEquals(item2, conflict1.getRight());

        Conflict conflict2 = new Conflict(item1, item2);
        assertEquals(conflict1, conflict2);
        assertEquals(conflict1.hashCode(), conflict2.hashCode());

        conflict2 = new Conflict(item2, item1);
        assertEquals(conflict1, conflict2);
        assertEquals(conflict1.hashCode(), conflict2.hashCode());

        ReadLaterItem item3 = new ReadLaterItem.Builder(item2).label("label3").build();
        conflict2 = new Conflict(item2, item3);
        assertFalse(conflict1.equals(conflict2));

        assertFalse(conflict1.equals(new Object()));
    }

    @Test
    public void testToString() {
        ReadLaterItem item1 = new ReadLaterItem.Builder("label").remoteId(1).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder("label2").remoteId(1).build();
        Conflict conflict = new Conflict(item1, item2);
        String conflictString = conflict.toString();
        // Должен содержать item.toString обоих объектов
        assertTrue(conflictString.contains(item1.toString()));
        assertTrue(conflictString.contains(item2.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEqualRemoteIdException() {
        ReadLaterItem item1 = new ReadLaterItem.Builder("label").remoteId(1).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder(item1).remoteId(0).build();
        assertEquals(null, new Conflict(item1, item2)); // throws exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEqualByContentException() {
        ReadLaterItem item1 = new ReadLaterItem.Builder("label").remoteId(1).build();
        ReadLaterItem item2 = new ReadLaterItem.Builder(item1).allDates(0).build();
        assertEquals(null, new Conflict(item1, item2)); // throws exception
    }

}
