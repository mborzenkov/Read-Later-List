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

    private static final int ONSTART_SLEEP = 3000;
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
    }

    @After
    public void tearDown() throws IOException {
        mServer.shutdown();
    }

    @Test
    public void testItemListFragmentIsDisplayed() {

        // Очистить данные
        ReadLaterDbUtils.deleteAll(mActivityTestRule.getActivity());

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


}
