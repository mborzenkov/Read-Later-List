package com.example.mborzenkov.readlaterlist.fragments.edititem;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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
import com.example.mborzenkov.readlaterlist.networking.DaggerCloudApiComponent;
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

/** Тестирует EditItemFragment. */
@RunWith(AndroidJUnit4.class)
public class EditItemFragmentTest {

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
    public void testEditItemFragmentEverythingDisplayed() {

        try {
            Thread.sleep(ONSTART_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isDisplayed()));
        fabAdd.perform(click());

        ViewInteraction labelInputLayout = onView(allOf(withId(R.id.til_edititem_label), isDisplayed()));
        labelInputLayout.check(matches(isDisplayed()));

        ViewInteraction color = onView(allOf(withId(R.id.ib_edit_item_color),
                withContentDescription("Color"), isDisplayed()));
        color.check(matches(isDisplayed()));

        ViewInteraction description = onView(allOf(withId(R.id.et_edit_item_description), isCompletelyDisplayed()));
        description.check(matches(isCompletelyDisplayed()));

        ViewInteraction imageUrl = onView(allOf(withId(R.id.et_edititem_imageurl),  isCompletelyDisplayed()));
        imageUrl.check(matches(isCompletelyDisplayed()));

        ViewInteraction reloadImageButton = onView(allOf(withId(R.id.ib_edititem_updateimage),
                withContentDescription("Image from url"), isDisplayed()));
        reloadImageButton.check(matches(isDisplayed()));

        ViewInteraction fabSave = onView(allOf(withId(R.id.fab_edititem_save), isDisplayed()));
        fabSave.check(matches(isDisplayed()));

        ViewInteraction toolbar = onView(allOf(withId(R.id.toolbar_edititem), isDisplayed()));
        toolbar.check(matches(isDisplayed()));

        ViewInteraction textView = onView(allOf(withId(R.id.edititem_action_save),
                withContentDescription("Save"), isDisplayed()));
        textView.check(matches(isDisplayed()));
    }

    @Test
    public void testAddingAndEditing() {

        deleteAllData();

        final String label1 = "abcdefg";
        final String desc1 = "hijklmnop";
        final String label2 = "qrstuvw";
        final String desc2 = "xyz";

        // Добавить заметку
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

        // Изменить
        {
            ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isCompletelyDisplayed()));
            itemList.perform(actionOnItemAtPosition(0, click()));

            ViewInteraction inputLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            inputLabel.perform(replaceText(label2), closeSoftKeyboard());

            ViewInteraction inputDesc = onView(allOf(withId(R.id.et_edit_item_description), isCompletelyDisplayed()));
            inputDesc.perform(replaceText(desc2), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.tv_item_label), withText(label2), isDisplayed()));
            itemLabel.check(matches(withText(label2)));
            ViewInteraction itemDesc = onView(allOf(withId(R.id.tv_item_description), withText(desc2), isDisplayed()));
            itemDesc.check(matches(withText(desc2)));
        }
    }

    @Test
    public void testViewPager() {

        deleteAllData();

        final String label1 = "LABLE1";
        final String label2 = "SECOOOND";
        final String label3 = "THRD";

        // Добавить 1
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isCompletelyDisplayed()));
            fabAdd.perform(click());

            ViewInteraction editLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            editLabel.perform(replaceText(label1), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        try {
            Thread.sleep(AFTER_ADD_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить 2
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isCompletelyDisplayed()));
            fabAdd.perform(click());

            ViewInteraction editLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            editLabel.perform(replaceText(label2), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        try {
            Thread.sleep(AFTER_ADD_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Добавить 3
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isCompletelyDisplayed()));
            fabAdd.perform(click());

            ViewInteraction editLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            editLabel.perform(replaceText(label3), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        // Выбрать серединку
        ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
        itemList.perform(actionOnItemAtPosition(1, click()));

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label), withText(label2),
                    isCompletelyDisplayed()));
            itemLabel.check(matches(withText(label2)));
        }

        // Свайп влево
        ViewInteraction viewPager = onView(allOf(withId(R.id.viewpager_edititem), isDisplayed()));
        viewPager.perform(swipeLeft());

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label), withText(label1),
                    isCompletelyDisplayed()));
            itemLabel.check(matches(withText(label1)));
        }

        // Свайп вправо
        viewPager.perform(swipeRight());

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label), withText(label2),
                    isCompletelyDisplayed()));
            itemLabel.check(matches(withText(label2)));
        }

        // Свайп вправо
        viewPager.perform(swipeRight());

        try {
            Thread.sleep(ANIM_SLEEP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверить
        {
            ViewInteraction itemLabel = onView(allOf(withId(R.id.et_edititem_label), withText(label3),
                    isCompletelyDisplayed()));
            itemLabel.check(matches(withText(label3)));
        }

    }

    @Test
    public void testDeleteItem() {

        deleteAllData();

        final String label = "SAMPLE_TEXT 123";

        // Добавить
        {
            ViewInteraction fabAdd = onView(allOf(withId(R.id.fab_item_add), isCompletelyDisplayed()));
            fabAdd.perform(click());

            ViewInteraction editLabel = onView(allOf(withId(R.id.et_edititem_label), isCompletelyDisplayed()));
            editLabel.perform(replaceText(label), closeSoftKeyboard());

            ViewInteraction menuSave = onView(allOf(withId(R.id.edititem_action_save), isCompletelyDisplayed()));
            menuSave.perform(click());
        }

        // Выбрать
        ViewInteraction itemList = onView(allOf(withId(R.id.listview_itemlist), isDisplayed()));
        itemList.perform(actionOnItemAtPosition(0, click()));

        // Удалить
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        ViewInteraction menuDelete = onView(allOf(withId(R.id.title), withText("Delete"), isDisplayed()));
        menuDelete.perform(click());
        ViewInteraction answerOk = onView(allOf(withId(android.R.id.button1), withText("OK")));
        answerOk.perform(scrollTo(), click());

        // Проверить
        {
            ViewInteraction item = onView(allOf(withId(R.id.tv_item_label), withText(label), isDisplayed()));
            item.check(doesNotExist());
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
