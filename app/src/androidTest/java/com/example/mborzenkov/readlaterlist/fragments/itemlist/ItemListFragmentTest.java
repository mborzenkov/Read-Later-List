package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.example.mborzenkov.readlaterlist.MyApplication;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.CloudApiMockDispatcher;
import com.example.mborzenkov.readlaterlist.networking.CloudApiModule;
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

/** Тестирует ItemListFragment. */
@RunWith(AndroidJUnit4.class)
public class ItemListFragmentTest {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    private static final int ONSTART_SLEEP = 2000;
    private static final int AFTER_ADD_SLEEP = 3500;
    private static final int ANIM_SLEEP = 500;

    private static final int USER_ID = 1005930;

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
    public void testItemListFragmentIsDisplayed() {

        deleteAllData();

        {
            ViewInteraction itemList = onView(allOf(withId(R.id.swiperefreshlayout_itemlist), isDisplayed()));
            itemList.check(matches(isDisplayed()));

            ViewInteraction toolBar = onView(allOf(withId(R.id.toolbar_itemlist), isDisplayed()));
            toolBar.check(matches(isDisplayed()));

            ViewInteraction searchButton = onView(allOf(withId(R.id.mainlist_action_search),
                    withContentDescription("Search"), isDisplayed()));
            searchButton.check(matches(isDisplayed()));

            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.check(matches(isDisplayed()));
            fabAdd.perform(click());
        }

        pressBack();

        {
            ViewInteraction itemListBack = onView(allOf(withId(R.id.swiperefreshlayout_itemlist), isDisplayed()));
            itemListBack.check(matches(isDisplayed()));

            ViewInteraction toolBarBack = onView(allOf(withId(R.id.toolbar_itemlist), isDisplayed()));
            toolBarBack.check(matches(isDisplayed()));

            ViewInteraction searchButtonBack = onView(allOf(withId(R.id.mainlist_action_search),
                    withContentDescription("Search"), isDisplayed()));
            searchButtonBack.check(matches(isDisplayed()));

            ViewInteraction fabAddBack = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAddBack.check(matches(isDisplayed()));
        }

    }

    @Test
    public void testSearchItems() {

        deleteAllData();

        final String label1 = "654321";
        final String desc1 = "descrepton 1";
        final String label2 = "098765";
        final String desc2 = "descororo 1";

        // Добавить 1
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.perform(click());

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(label1), closeSoftKeyboard());

            ViewInteraction inputDesc = onView(allOf(withId(R.id.et_edit_item_description), isCompletelyDisplayed()));
            inputDesc.perform(replaceText(desc1), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save),
                    withContentDescription("Save"), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            itemLabel.check(matches(withText(label1)));
            ViewInteraction itemDesc = onView(allOf(withId(R.id.tv_item_description), withText(desc1), isDisplayed()));
            itemDesc.check(matches(withText(desc1)));
        }

        try {
            Thread.sleep(AFTER_ADD_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить 2
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.perform(click());

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(label2), closeSoftKeyboard());

            ViewInteraction inputDesc = onView(allOf(withId(R.id.et_edit_item_description), isCompletelyDisplayed()));
            inputDesc.perform(replaceText(desc2), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save),
                    withContentDescription("Save"), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            itemLabel.check(matches(withText(label2)));
            ViewInteraction itemDesc = onView(allOf(withId(R.id.tv_item_description), withText(desc2), isDisplayed()));
            itemDesc.check(matches(withText(desc2)));
        }

        try {
            Thread.sleep(AFTER_ADD_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Открываем поиск
        ViewInteraction search = onView(allOf(withId(R.id.mainlist_action_search),
                withContentDescription("Search"), isDisplayed()));
        search.perform(click());

        // Ищем лейбл 1
        {
            ViewInteraction searchAutoComplete = onView(allOf(withId(R.id.search_src_text), isDisplayed()));
            searchAutoComplete.perform(replaceText(label1), closeSoftKeyboard());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            item1.check(matches(withText(label1)));
            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            item2.check(doesNotExist());
        }

        // Очищаем поиск
        ViewInteraction appCompatImageView = onView(allOf(withId(R.id.search_close_btn),
                withContentDescription("Clear query"), isDisplayed()));
        appCompatImageView.perform(click());

        // Ищем описание 2
        {
            ViewInteraction searchAutoComplete = onView(allOf(withId(R.id.search_src_text), isDisplayed()));
            searchAutoComplete.perform(replaceText(desc2), closeSoftKeyboard());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText(label1), isDisplayed()));
            item1.check(doesNotExist());
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

}
