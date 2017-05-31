package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.example.mborzenkov.readlaterlist.MyApplication;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.CloudApiMockDispatcher;
import com.example.mborzenkov.readlaterlist.networking.CloudApiModule;
import com.example.mborzenkov.readlaterlist.networking.DaggerCloudApiComponent;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ItemListFragmentTest {

    private static final int ONSTART_SLEEP = 2000;
    private static final int AFTER_ADD_SLEEP = 3500;
    private static final int ANIM_SLEEP = 500;

    private static final int USER_ID = 1005930;
    private MockWebServer mServer = new MockWebServer();
    private HttpUrl mServerUrl;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws IOException {
        mServer.start();
        mServerUrl = mServer.url("");
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyDispatcher());
        CloudApiComponent component = DaggerCloudApiComponent.builder()
                .cloudApiModule(new CloudApiModule(mServerUrl)).build();
        MyApplication application = (MyApplication) mActivityTestRule.getActivity().getApplication();
        application.setCloudApiComponent(component);
        UserInfoUtils.changeCurrentUser(application, USER_ID);
        ReadLaterDbUtils.deleteAll(application);
    }

    @After
    public void tearDown() throws IOException {
        mServer.shutdown();
    }

    @Test
    public void testItemListFragmentIsDisplayed() {

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
    public void testAddItemOpenItem() {

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
        fabAdd.check(matches(isDisplayed()));
        fabAdd.perform(click());

        ViewInteraction labelInput = onView(allOf(withId(R.id.et_edititem_label), isDisplayed()));
        labelInput.perform(replaceText("654321"), closeSoftKeyboard());

        ViewInteraction fabSave = onView(allOf(withId(R.id.fab_edititem_save), isDisplayed()));
        fabSave.perform(click());

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction labelInList = onView(allOf(withId(R.id.tv_item_label), withText("654321"), isDisplayed()));
        labelInList.check(matches(withText("654321")));

        ViewInteraction listView = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
        listView.perform(actionOnItemAtPosition(0, click()));

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction labelInputOpened = onView(allOf(withId(R.id.et_edititem_label),
                withText("654321"), isDisplayed()));
        labelInputOpened.check(matches(withText("654321")));
    }

    @Test
    public void testSearchItems() {
        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить 1
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.check(matches(isDisplayed()));
            fabAdd.perform(click());

            ViewInteraction labelInput = onView(allOf(withId(R.id.et_edititem_label), isDisplayed()));
            labelInput.perform(replaceText("654321"), closeSoftKeyboard());

            ViewInteraction fabSave = onView(allOf(withId(R.id.fab_edititem_save), isDisplayed()));
            fabSave.perform(click());

            try {
                Thread.sleep(AFTER_ADD_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction labelInList = onView(allOf(withId(R.id.tv_item_label), withText("654321"), isDisplayed()));
            labelInList.check(matches(withText("654321")));
        }

        // Добавить 2
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
            fabAdd.check(matches(isDisplayed()));
            fabAdd.perform(click());

            ViewInteraction labelInput = onView(allOf(withId(R.id.et_edititem_label), isDisplayed()));
            labelInput.perform(replaceText("098765"), closeSoftKeyboard());

            ViewInteraction fabSave = onView(allOf(withId(R.id.fab_edititem_save), isDisplayed()));
            fabSave.perform(click());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction labelInList = onView(allOf(withId(R.id.tv_item_label), withText("098765"), isDisplayed()));
            labelInList.check(matches(withText("098765")));
        }

        // Проверить что все на месте
        {
            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText("654321"), isDisplayed()));
            item1.check(matches(withText("654321")));
            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText("098765"), isDisplayed()));
            item2.check(matches(withText("098765")));
        }

        // Открываем поиск
        ViewInteraction search = onView(allOf(withId(R.id.mainlist_action_search),
                withContentDescription("Search"), isDisplayed()));
        search.perform(click());

        // Ищем 1
        {
            ViewInteraction searchAutoComplete = onView(allOf(withId(R.id.search_src_text), isDisplayed()));
            searchAutoComplete.perform(replaceText("654321"), closeSoftKeyboard());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText("654321"), isDisplayed()));
            item1.check(matches(withText("654321")));
            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText("098765"), isDisplayed()));
            item2.check(doesNotExist());
        }

        // Очищаем поиск
        ViewInteraction appCompatImageView = onView(allOf(withId(R.id.search_close_btn),
                withContentDescription("Clear query"), isDisplayed()));
        appCompatImageView.perform(click());

        // Ищем 1
        {
            ViewInteraction searchAutoComplete = onView(allOf(withId(R.id.search_src_text), isDisplayed()));
            searchAutoComplete.perform(replaceText("098765"), closeSoftKeyboard());

            try {
                Thread.sleep(ANIM_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViewInteraction item1 = onView(allOf(withId(R.id.tv_item_label), withText("654321"), isDisplayed()));
            item1.check(doesNotExist());
            ViewInteraction item2 = onView(allOf(withId(R.id.tv_item_label), withText("098765"), isDisplayed()));
            item2.check(matches(withText("098765")));
        }
    }

}
