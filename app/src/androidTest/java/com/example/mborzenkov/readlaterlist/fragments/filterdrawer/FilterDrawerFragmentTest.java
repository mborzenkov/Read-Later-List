package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.example.mborzenkov.readlaterlist.MyApplication;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.CloudApiMockDispatcher;
import com.example.mborzenkov.readlaterlist.networking.CloudApiModule;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Тестирует FilterDrawerFragment. */
@SuppressWarnings("unchecked")
@RunWith(AndroidJUnit4.class)
public class FilterDrawerFragmentTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int ONSTART_SLEEP = 2000;
    private static final int AFTER_ADD_SLEEP = 3500;
    private static final int ANIM_SLEEP = 500;

    private static final int PLACEHOLDERS_COUNT = 5;

    private static final int USER_ID = 1005930;
    private static final int USER_ID_SECOND = 100593;

    private final MockWebServer mServer = new MockWebServer();

    private MainActivity mActivity;

    @Rule
    public final ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    /** Создает, запускае и подключает fake-сервер, устанавливает текущего пользователя. */
    @Before
    public void setUp() throws IOException {
        mServer.start();
        HttpUrl serverUrl = mServer.url("");
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyDispatcher());
        mActivity = mActivityTestRule.getActivity();
        CloudApiComponent component = DaggerCloudApiComponent.builder()
                .cloudApiModule(new CloudApiModule(serverUrl)).build();
        MyApplication application = (MyApplication) mActivity.getApplication();
        application.setCloudApiComponent(component);
        UserInfoUtils.changeCurrentUser(application, USER_ID);
    }

    @After
    public void tearDown() throws IOException {
        mServer.shutdown();
    }

    @Test
    public void testFilterFillPlaceholdersDeleteAll() {

        deleteAllData();

        final int placeholdersCount = 5;

        {
            // Открываем дровер, кликаем фил плейсхолдерс
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction fillPlaceholders = onView(allOf(withId(R.id.button_filterdrawer_fillplaceholders)));
            fillPlaceholders.perform(scrollTo(), click());

            ViewInteraction editText = onView(allOf(withClassName(is("android.widget.EditText")), isDisplayed()));
            editText.perform(replaceText(String.valueOf(placeholdersCount)), closeSoftKeyboard());

            ViewInteraction actionOk = onView(allOf(withId(android.R.id.button1), withText("OK")));
            actionOk.perform(scrollTo(), click());

            try {
                Thread.sleep(ONSTART_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что добавились данные
            Cursor cursor = ReadLaterDbUtils.queryAllItems(mActivity, USER_ID);
            assertTrue(cursor != null);
            assertEquals(placeholdersCount, cursor.getCount());
            cursor.close();
        }

        {
            // Открываем дровер, кликаем delete all
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction deleteAll = onView(allOf(withId(R.id.button_filterdrawer_deleteall)));
            deleteAll.perform(scrollTo(), click());

            ViewInteraction actionOk = onView(allOf(withId(android.R.id.button1), withText("OK")));
            actionOk.perform(scrollTo(), click());

            try {
                Thread.sleep(ONSTART_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что удалились данные
            Cursor cursor = ReadLaterDbUtils.queryAllItems(mActivity, USER_ID);
            assertTrue(cursor != null);
            assertEquals(0, cursor.getCount());
            cursor.close();
        }
    }

    @Test
    public void testFilterChangeUser() {

        deleteAllAndFillPlaceholders();

        {
            // Смотрим, что появились данные
            ViewInteraction textView = onView(allOf(withId(R.id.tv_item_label),
                    withText("Labeeeeel 4"), isDisplayed()));
            textView.check(matches(withText("Labeeeeel 4")));
        }

        {
            // Открываем дровер, кликаем change, меняем пользователя
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction changeUser = onView(allOf(withId(R.id.tv_filterdrawer_user_change)));
            changeUser.perform(scrollTo(), click());

            ViewInteraction enterUserField = onView(allOf(withClassName(is("android.widget.EditText")), isDisplayed()));
            enterUserField.perform(replaceText(String.valueOf(USER_ID_SECOND)), closeSoftKeyboard());

            ViewInteraction appCompatButton3 = onView(allOf(withId(android.R.id.button1), withText("OK")));
            appCompatButton3.perform(scrollTo(), click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что данные исчезли
            ViewInteraction textView = onView(allOf(withId(R.id.tv_item_label),
                    withText("Labeeeeel 4"), isDisplayed()));
            textView.check(doesNotExist());
        }

        {
            // Открываем дровер, кликаем change, меняем пользователя обратно
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction changeUser = onView(allOf(withId(R.id.tv_filterdrawer_user_change)));
            changeUser.perform(scrollTo(), click());

            ViewInteraction enterUserField = onView(allOf(withClassName(is("android.widget.EditText")), isDisplayed()));
            enterUserField.perform(replaceText(String.valueOf(USER_ID)), closeSoftKeyboard());

            ViewInteraction appCompatButton3 = onView(allOf(withId(android.R.id.button1), withText("OK")));
            appCompatButton3.perform(scrollTo(), click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что данные появились
            ViewInteraction textView = onView(allOf(withId(R.id.tv_item_label),
                    withText("Labeeeeel 4"), isDisplayed()));
            textView.check(matches(withText("Labeeeeel 4")));
        }
    }

    @Test
    public void testBackupSaveRestore() {

        // Сохранение, восстановление только external storage
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // TODO: Добавить обработку permission
            return;
        }

        deleteAllAndFillPlaceholders();

        {
            // Открываем дровер, кликаем Backup save
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction backupSave = onView(allOf(withId(R.id.button_filterdrawer_backupsave)));
            backupSave.perform(scrollTo(), click());

            ViewInteraction backupSaveConfirm = onView(allOf(withId(android.R.id.button1), withText("OK")));
            backupSaveConfirm.perform(scrollTo(), click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));
        }

        // Очищаем все данные
        deleteAllData();

        {
            // Открываем дровер, кликаем Backup restore
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction backupSave = onView(allOf(withId(R.id.button_filterdrawer_backuprestore)));
            backupSave.perform(scrollTo(), click());

            ViewInteraction backupSaveConfirm = onView(allOf(withId(android.R.id.button1), withText("OK")));
            backupSaveConfirm.perform(scrollTo(), click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что добавились данные
            Cursor cursor = ReadLaterDbUtils.queryAllItems(mActivity, USER_ID);
            assertTrue(cursor != null);
            assertEquals(PLACEHOLDERS_COUNT, cursor.getCount());
            cursor.close();
        }

    }

    @Test
    public void testChangeOrderLabel() {

        deleteAllAndFillPlaceholders();

        final int firstPosition = 0;
        final int lastPosition = 4;
        final String elementLabel = "Labeeeeel 4";

        {
            // Открываем дровер, кликаем на сортировку по лейблу
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortName = onView(allOf(withId(R.id.button_filterdrawer_sortname)));
            sortName.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что последний элемент это последний по имени
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
            itemList.perform(actionOnItemAtPosition(lastPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));

            ViewInteraction appCompatImageButton = onView(allOf(withContentDescription("Navigate up"),
                    isCompletelyDisplayed()));
            appCompatImageButton.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        {
            // Открываем дровер, кликаем на сортировку по лейблу еще раз
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortName = onView(allOf(withId(R.id.button_filterdrawer_sortname)));
            sortName.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что первый элемент это последний по имени
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist),
                    isCompletelyDisplayed()));
            itemList.perform(actionOnItemAtPosition(firstPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));
        }
    }

    @Test
    public void testChangeOrderModified() {

        deleteAllAndFillPlaceholders();

        final int firstPosition = 0;
        final int lastPosition = 4;
        final String elementLabel = "perfect label";

        {
            // Открываем элемент, меняем его
            ViewInteraction labelInputOpened = onView(allOf(withId(R.id.tv_item_label),
                    withText("Labeeeeel 4"), isDisplayed()));
            labelInputOpened.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(elementLabel), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());

            try {
                Thread.sleep(ONSTART_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        {
            // Открываем дровер, кликаем на сортировку по дате изменения
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortModified = onView(allOf(withId(R.id.button_filterdrawer_sortmodified)));
            sortModified.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что последний элемент это последний измененный
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
            itemList.perform(actionOnItemAtPosition(lastPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));

            ViewInteraction appCompatImageButton = onView(allOf(withContentDescription("Navigate up"),
                    isCompletelyDisplayed()));
            appCompatImageButton.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        {
            // Открываем дровер, кликаем на сортировку по дате изменения еще раз
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortModified = onView(allOf(withId(R.id.button_filterdrawer_sortmodified)));
            sortModified.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что первый элемент это последний измененный
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
            itemList.perform(actionOnItemAtPosition(firstPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));
        }
    }

    @Test
    public void testChangeOrderViewed() {

        deleteAllAndFillPlaceholders();

        final int firstPosition = 0;
        final int lastPosition = 4;
        final String elementLabel = "Labeeeeel 4";

        {
            // Открываем элемент, закрываем
            ViewInteraction labelInputOpened = onView(allOf(withId(R.id.tv_item_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            labelInputOpened.perform(click());

            ViewInteraction appCompatImageButton = onView(allOf(withContentDescription("Navigate up"),
                    isCompletelyDisplayed()));
            appCompatImageButton.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        {
            // Открываем дровер, кликаем на сортировку по дате просмотра
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortView = onView(allOf(withId(R.id.button_filterdrawer_sortview)));
            sortView.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что последний элемент это последний просмотренный
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
            itemList.perform(actionOnItemAtPosition(lastPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));

            ViewInteraction appCompatImageButton = onView(allOf(withContentDescription("Navigate up"),
                    isCompletelyDisplayed()));
            appCompatImageButton.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        {
            // Открываем дровер, кликаем на сортировку по дате просмотра еще раз
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            ViewInteraction sortView = onView(allOf(withId(R.id.button_filterdrawer_sortview)));
            sortView.perform(scrollTo(), click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Смотрим, что первый элемент это последний измененный
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
            itemList.perform(actionOnItemAtPosition(firstPosition, click()));

            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label),
                    withText(elementLabel), isCompletelyDisplayed()));
            itemLabel.check(matches(withText(elementLabel)));
        }
    }

    @Test
    public void testFilterByColor() {

        deleteAllData();

        final String label1 = "label test case";
        final String label2 = "label, just label";

        // Добавить заметку 1
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isCompletelyDisplayed()));
            fabAdd.perform(click());

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(label1), closeSoftKeyboard());

            // Открываем цветовыбиратель
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isCompletelyDisplayed()));
            itemColor.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 0)))).perform(click());

            // Сохраняем в Favorites
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 0)))).perform(longClick());

            // Сохраняем заметку
            ViewInteraction chosenColor = onView(allOf(withId(R.id.imageButton_chosen),
                    withContentDescription("Choose color"), isCompletelyDisplayed()));
            chosenColor.perform(click());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save),
                    withContentDescription("Save"), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        try {
            Thread.sleep(AFTER_ADD_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить заметку 2
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.perform(click());

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(label2), closeSoftKeyboard());

            // Открываем цветовыбиратель
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isCompletelyDisplayed()));
            itemColor.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 1)))).perform(click());

            // Сохраняем в Favorites
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 1)))).perform(longClick());

            // Сохраняем заметку
            ViewInteraction chosenColor = onView(allOf(withId(R.id.imageButton_chosen),
                    withContentDescription("Choose color"), isDisplayed()));
            chosenColor.perform(click());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save),
                    withContentDescription("Save"), isDisplayed()));
            menuSave.perform(click());
        }

        {
            // Открываем дровер, кликаем первый цвет
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 0)))).perform(click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что выводится только 1 заметка
            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            item1.check(matches(withText(label1)));

            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            item2.check(doesNotExist());
        }

        {
            // Открываем дровер, кликаем первый цвет и потом второй
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 0)))).perform(click());
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 1)))).perform(click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что выводится только 2 заметка
            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            item1.check(doesNotExist());

            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            item2.check(matches(withText(label2)));
        }

        {
            // Открываем дровер, кликаем второй цвет
            ViewInteraction drawerLayout = onView(allOf(withId(R.id.drawerlayout_itemlist)));
            drawerLayout.perform(DrawerActions.open(Gravity.END));

            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 1)))).perform(click());

            drawerLayout.perform(DrawerActions.close(Gravity.END));

            // Смотрим, что снова все выводится
            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            item1.check(matches(withText(label1)));

            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            item2.check(matches(withText(label2)));
        }
    }

    private void deleteAllData() {
        // Очистить данные
        ReadLaterDbUtils.deleteAll(mActivity);

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Перезагрузить данные
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onDataChanged();
            }
        });

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void deleteAllAndFillPlaceholders() {
        // Очистить данные
        ReadLaterDbUtils.deleteAll(mActivity);

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить плейсхолдеры
        final int placeholdersCount = 5;
        DebugUtils.addPlaceholdersToDatabase(mActivity, placeholdersCount);

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Перезагрузить данные
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onDataChanged();
            }
        });


        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
