package com.example.mborzenkov.readlaterlist.fragments;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

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
import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ColorPickerFragmentTest {

    private static final int ONSTART_SLEEP = 2000;
    private static final int TAKE_A_NAP = 250;

    private final MockWebServer mServer = new MockWebServer();

    @Rule
    public final ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws IOException {
        mServer.start();
        HttpUrl serverUrl = mServer.url("");
        mServer.setDispatcher(new CloudApiMockDispatcher.EmptyDispatcher());
        CloudApiComponent component = DaggerCloudApiComponent.builder()
                .cloudApiModule(new CloudApiModule(serverUrl)).build();
        MyApplication application = (MyApplication) mActivityTestRule.getActivity().getApplication();
        application.setCloudApiComponent(component);
        // В этих тестах не создаются новые заметки
        // UserInfoUtils.changeCurrentUser(application, USER_ID);
        // ReadLaterDbUtils.deleteAll(application);
    }

    @After
    public void tearDown() throws IOException {
        mServer.shutdown();
    }

    @Test
    public void testChooseColor() {
        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Открываем редактирование
        ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
        fabAdd.perform(click());

        // Открываем цветовыбиратель
        {
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isDisplayed()));
            itemColor.perform(click());

            try {
                Thread.sleep(TAKE_A_NAP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 1)))).perform(click());

            ViewInteraction chosenColor = onView(allOf(withId(R.id.imageButton_chosen),
                    withContentDescription("Choose color"), isDisplayed()));
            chosenColor.perform(click());
        }

        // Открываем еще раз цветовыбиратель
        {
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isDisplayed()));
            itemColor.perform(click());

            try {
                Thread.sleep(TAKE_A_NAP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 2)))).perform(click());

            ViewInteraction chosenColor = onView(allOf(withId(R.id.imageButton_chosen),
                    withContentDescription("Choose color"), isDisplayed()));
            chosenColor.perform(click());
        }

        // Сохраняем
        ViewInteraction labelInput = onView(
                allOf(withId(R.id.et_edititem_label), isDisplayed()));
        labelInput.perform(replaceText("123"), closeSoftKeyboard());

        ViewInteraction fabSave = onView(
                allOf(withId(R.id.fab_edititem_save), isDisplayed()));
        fabSave.perform(click());
    }

    @Test
    public void testSaveToFavorites() {
        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Открываем редактирование
        ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
        fabAdd.perform(click());

        // Открываем цветовыбиратель
        {
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isDisplayed()));
            itemColor.perform(click());

            try {
                Thread.sleep(TAKE_A_NAP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 2)))).perform(click());

            // Сохраняем в Favorites
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 0)))).perform(longClick());

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 1)))).perform(click());

            // Сохраняем в Favorites
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 1)))).perform(longClick());

            // Выбираем цвет
            onView(allOf(withId(R.id.imageButton_colored_square), withTagValue(equalTo((Object) 0)))).perform(click());

            // Сохраняем в Favorites
            onView(allOf(withId(R.id.imageButton_favorite_color),
                    withTagValue(equalTo((Object) 2)))).perform(longClick());

            // Сохраняем
            ViewInteraction chosenColor = onView(allOf(withId(R.id.imageButton_chosen),
                    withContentDescription("Choose color"), isDisplayed()));
            chosenColor.perform(click());
        }

        // Открываем еще раз цветовыбиратель
        {
            ViewInteraction itemColor = onView(allOf(withId(R.id.ib_edit_item_color),
                    withContentDescription("Color"), isDisplayed()));
            itemColor.perform(click());
        }
    }

}
