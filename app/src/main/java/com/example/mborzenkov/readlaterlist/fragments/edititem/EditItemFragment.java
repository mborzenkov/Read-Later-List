package com.example.mborzenkov.readlaterlist.fragments.edititem;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.utility.view.ActivityUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Fragment для редактирования элемента.
 * Использование:
 *      Для получения объекта всегда используйте getInstance.
 *      Для заполнения фрагмента данными, необходимо передать в getInstance объект ReadLaterItem и его itemLocalId.
 *      Для получения результатов редактирования, необходимо, чтобы Activity, использующая фрагмент, реализовывала
 *          интерфейс EditItemCallbacks.
 *      Для использования shared element необходимо вызывать setSharedElementTransitionName с уникальным именем
 *              и поддерживать уникальность во всей иерархии View (например, для ViewPager - устанавливать у текущего и
 *              стирать у остальных).
 *      Menu устанавливается автоматически
 */
public class EditItemFragment extends Fragment implements
        View.OnClickListener,
        EditItemFragmentActions {

    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    @SuppressWarnings("WeakerAccess") // на будущее
    public static final String TAG = "fragment_edititem";

    // Ключи для SavedInstanceState
    private static final String SAVEDINSTANCE_COLOR_KEY = "edititem_color";
    private static final String SAVEDINSTANCE_DATECREATED_KEY = "edititem_datecreated";
    private static final String SAVEDINSTANCE_DATEMODIFIED_KEY = "edititem_datemodified";

    /** Формат дат. */
    private static final String FORMAT_DATE = "dd.MM.yy HH:mm";

    /** Ошибка при нарушении инварианта. */
    private static final String ERROR_INVARIANT_FAIL =
            "EditItemFragment invariant break: mFromItem = %s, mFromItemLocalId = %s";


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект EditItemFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @param item объект для редактирования или null, если создание нового элемента
     * @param itemLocalId внутренний идентификатор объекта или UID_EMPTY, если создание нового элемента
     * @return новый объект EditItemFragment
     * @throws IllegalArgumentException если itemLocalId < UID_EMPTY
     * @throws IllegalArgumentException если item == null и itemLocalId != -UID_EMPTY
     */
    public static EditItemFragment getInstance(FragmentManager fragmentManager,
                                               @Nullable ReadLaterItem item,
                                               @IntRange(from = UID_EMPTY) int itemLocalId) {

        if ((itemLocalId < UID_EMPTY) || ((item == null) && (itemLocalId != UID_EMPTY))) {
            throw new IllegalArgumentException(
                    String.format("Erorr @ EditItemFragment.getInstance. itemLocalId: %s, item: %s",
                            itemLocalId, item));
        }

        EditItemFragment fragment = (EditItemFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new EditItemFragment();
        }

        if (item != null) {
            Bundle args = new Bundle();
            args.putParcelable(BUNDLE_ITEM_KEY, new ReadLaterItemParcelable(item));
            args.putInt(BUNDLE_ITEMID_KEY, itemLocalId);
            fragment.setArguments(args);
        } else {
            fragment.setArguments(null);
        }

        return fragment;

    }


    /////////////////////////
    // Поля объекта

    // Инвариант
    //      mFromItem - объект, редактирование которого производится или null, если создание нового
    //      mFromItemLocalId - внутренний идентификатор mFromItem или UID_EMPTY, если создание нового объекта

    /** Редактируемый элемент. */
    private @Nullable ReadLaterItem mFromItem = null;

    /** Внутренний идентификатор редактируемого объекта. */
    private @IntRange(from = UID_EMPTY) int mFromItemLocalId = UID_EMPTY;

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable EditItemCallbacks mCallbacks = null;

    /** Текущий выбранный цвет в формате sRGB. */
    private int mChosenColor;

    /** Имя transition для shared element.
     * Должно быть установлено для отображаемого фрагмента.
     * Должно быть null для всех фрагментов кроме текущего во ViewPager.
     */
    private @Nullable String mTransitionName = null;

    // Объекты layout
    private TextInputEditText mLabelEditText;
    private TextInputLayout mLabelInputLayout;
    private TextInputEditText mDescriptionEditText;
    private ImageView mColorImageButton;
    private TextInputEditText mImageUrlEditText;
    private TextInputLayout mImageUrlInputLayout;
    private ImageView mImageFromUrlImageView;


    /////////////////////////
    // Проверка достоверности инварианта

    private void checkRep() {
        if (BuildConfig.DEBUG) {
            if (mFromItemLocalId < UID_EMPTY) {
                throw new AssertionError(String.format(ERROR_INVARIANT_FAIL, mFromItem, mFromItemLocalId));
            } else if ((mFromItem == null) && (mFromItemLocalId != UID_EMPTY)) {
                throw new AssertionError(String.format(ERROR_INVARIANT_FAIL, "null", mFromItemLocalId));
            }
        }
    }


    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EditItemCallbacks) {
            mCallbacks = (EditItemCallbacks) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            ReadLaterItemParcelable itemParcelable = args.getParcelable(BUNDLE_ITEM_KEY);
            mFromItem = itemParcelable == null ? null : itemParcelable.getItem();
            mFromItemLocalId = args.getInt(BUNDLE_ITEMID_KEY, UID_EMPTY);
        }
        checkRep();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_edititem, container, false);

        // Инициализация объектов layout
        mLabelEditText = (TextInputEditText) rootView.findViewById(R.id.et_edititem_label);
        mLabelInputLayout = (TextInputLayout) rootView.findViewById(R.id.til_edititem_label);
        mDescriptionEditText = (TextInputEditText) rootView.findViewById(R.id.et_edit_item_description);
        mColorImageButton = (ImageView) rootView.findViewById(R.id.ib_edit_item_color);
        mImageUrlEditText = (TextInputEditText) rootView.findViewById(R.id.et_edititem_imageurl);
        mImageUrlInputLayout = (TextInputLayout) rootView.findViewById(R.id.til_edititem_imageurl);
        mImageFromUrlImageView = (ImageView) rootView.findViewById(R.id.iv_edititem_imagefromurl);

        // Инициализация FAB Save
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab_edititem_save);
        fab.setOnClickListener(this);

        // Установка клик листенеров
        mColorImageButton.setOnClickListener(this);
        rootView.findViewById(R.id.ib_edititem_updateimage).setOnClickListener(this);
        mLabelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mLabelInputLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Заполнение данными
        if (savedInstanceState == null) {

            if (mFromItem != null) {
                reloadDataFromItem(rootView);
                fab.setImageResource(R.drawable.ic_edit_24dp);
            } else {
                setColor(ContextCompat.getColor(getContext(), R.color.item_default_color));
                fab.setImageResource(R.drawable.ic_add_24dp);
            }

            // Устанавливаем фокус и открываем клавиатуру на редактирование Label, чтобы все было красиво и удобно
            if (mLabelEditText.requestFocus()) {
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }

        } else {
            setColor(savedInstanceState.getInt(SAVEDINSTANCE_COLOR_KEY,
                    ContextCompat.getColor(getContext(), R.color.item_default_color)));
            ((TextView) rootView.findViewById(R.id.tv_edititem_created_value))
                    .setText(savedInstanceState.getString(SAVEDINSTANCE_DATECREATED_KEY, ""));
            ((TextView) rootView.findViewById(R.id.tv_edititem_modified_value))
                    .setText(savedInstanceState.getString(SAVEDINSTANCE_DATEMODIFIED_KEY, ""));
            if (mFromItem == null) {
                fab.setImageResource(R.drawable.ic_edit_24dp);
            } else {
                fab.setImageResource(R.drawable.ic_add_24dp);
            }
        }

        reloadMenu(rootView);

        // Есть меню
        setHasOptionsMenu(true);

        return rootView;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mTransitionName != null) {
            ViewCompat.setTransitionName(mColorImageButton, mTransitionName);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVEDINSTANCE_COLOR_KEY, mChosenColor);
        if (getView() != null) {
            outState.putString(SAVEDINSTANCE_DATECREATED_KEY,
                    ((TextView) getView().findViewById(R.id.tv_edititem_created_value)).getText().toString());
            outState.putString(SAVEDINSTANCE_DATEMODIFIED_KEY,
                    ((TextView) getView().findViewById(R.id.tv_edititem_modified_value)).getText().toString());
        }
        // А все остальное сохраняется само по себе
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setHasOptionsMenu(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    /////////////////////////
    // Колбеки Menu

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_edititem, menu);

        if (mFromItem == null) {
            // Кнопка удаления нужна только для редактируемых
            menu.removeItem(R.id.edititem_action_delete);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.edititem_action_delete:
                ActivityUtils.showAlertDialog(
                        getContext(),
                        getString(R.string.edititem_menu_delete_question_title),
                        getString(R.string.edititem_menu_delete_question_text),
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mCallbacks != null) {
                                    if (mFromItemLocalId > UID_EMPTY) {
                                        mCallbacks.onDeleteItem(mFromItemLocalId);
                                    } else {
                                        mCallbacks.onExitWithoutModifying(mFromItem, mFromItemLocalId);
                                    }
                                }
                            }
                        },
                        null);
                return true;
            case R.id.edititem_action_save:
                closeWithSaving();
                return true;
            case android.R.id.home:
                onExitAttempt();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /////////////////////////
    // Колбеки View.onClickListener

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.ib_edit_item_color:
                if (view.getId() == R.id.ib_edit_item_color) {
                    if (mCallbacks != null) {
                        mCallbacks.onRequestColorPicker(mChosenColor, mColorImageButton);
                    }
                }
                break;
            case R.id.fab_edititem_save:
                closeWithSaving();
                break;
            case R.id.ib_edititem_updateimage:
                reloadImage();
                break;
            default:
                break;
        }
    }

    /////////////////////////
    // Методы при заершении редактирования

    /** Выполняет закрытие фрагмента с сохранением. */
    private void closeWithSaving() {
        if (!isModified()) {
            if (mCallbacks != null) {
                mCallbacks.onExitWithoutModifying(mFromItem, mFromItemLocalId);
            }
            return;
        }
        // packInputData возвращает null, если что-то не так
        ReadLaterItem resultData = packInputData();
        if ((resultData != null) && (mCallbacks != null)) {
            if (mFromItem == null) {
                mCallbacks.onCreateNewItem(resultData);
            } else {
                // Ошибка проверки, mFromItemLocalId > UID_EMPTY подтверждается инвариантом, если mFromItem != null
                //noinspection Range
                mCallbacks.onSaveItem(resultData, mFromItemLocalId);
            }
        }
    }

    /** Вызывается при попытке выйти из фрагмента без сохранения. */
    private void onExitAttempt() {
        if (isModified()) {
            showModifiedAlertWithOptions(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCallbacks != null) {
                                mCallbacks.onExitWithoutModifying(mFromItem, mFromItemLocalId);
                            }
                        }
                    },
                    null);
        } else {
            if (mCallbacks != null) {
                mCallbacks.onExitWithoutModifying(mFromItem, mFromItemLocalId);
            }
        }
    }


    /////////////////////////
    // Колбеки EditItemFragmentActions

    @Override
    public void onBackPressed() {
        onExitAttempt();
    }

    @Override
    public void setColor(int newColor) {
        mChosenColor = newColor;
        ((GradientDrawable) mColorImageButton.getBackground()).setColor(mChosenColor);
    }


    /////////////////////////
    // Вспомогательные методы

    /** Выполняет перезагрузку изображения по введенному url. */
    private void reloadImage() {

        // Проверяем, url ли там вообще
        String url = mImageUrlEditText.getText().toString();
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            mImageUrlInputLayout.setError(getString(R.string.edititem_error_imageurl_malformed));
            return; // Это не url
        }

        Picasso.with(getContext()).load(url).into(mImageFromUrlImageView, new Callback() {
            @Override
            public void onSuccess() {
                mImageUrlInputLayout.setError(null);
            }

            @Override
            public void onError() {
                mImageUrlInputLayout.setError(getString(R.string.edititem_error_imageurl_onload));
            }
        });

    }

    /** Проверяет, были ли изменения.
     *
     * @return true, если изменения были, иначе false
     */
    boolean isModified() {
        if (mFromItem != null) {
            ReadLaterItem thisItem = packInputData();
            return thisItem == null
                    || mFromItem.getColor() != thisItem.getColor()
                    || !mFromItem.getLabel().equals(thisItem.getLabel())
                    || !mFromItem.getDescription().equals(thisItem.getDescription())
                    || !mFromItem.getImageUrl().equals(thisItem.getImageUrl());
        }
        return mChosenColor != ContextCompat.getColor(getContext(), R.color.item_default_color)
                || !mLabelEditText.getText().toString().trim().isEmpty()
                || !mDescriptionEditText.getText().toString().trim().isEmpty()
                || !mImageUrlEditText.getText().toString().trim().isEmpty();
    }

    /** Превращает данные формы в объект ReadLaterItem.
     *
     * @return Объект ReadLaterItem или null, если не удалось превратить данные в объект
     *      Например, если не заполнен Label
     */
    private @Nullable ReadLaterItem packInputData() {
        String label = mLabelEditText.getText().toString().trim();
        String description = mDescriptionEditText.getText().toString().trim();
        String imageUrl = mImageUrlEditText.getText().toString().trim();
        if (!imageUrl.isEmpty()) {
            try {
                new URL(imageUrl);
            } catch (MalformedURLException e) {
                mImageUrlInputLayout.setError(getString(R.string.edititem_error_imageurl_malformed));
                return null; // Это не url
            }
        }
        if (!label.isEmpty()) {

            ReadLaterItem.Builder resultBuilder = new ReadLaterItem.Builder(label)
                    .description(description)
                    .color(mChosenColor)
                    .imageUrl(imageUrl);

            if (mFromItem != null) {
                resultBuilder.dateCreated(mFromItem.getDateCreated());
                resultBuilder.remoteId(mFromItem.getRemoteId());
            }

            return resultBuilder.build();

        } else {
            mLabelInputLayout.setError(getString(R.string.edititem_error_title_empty));
            return null;
        }
    }

    /** Устанавливает имя для Shared Element.
     * Если фрагмент уже прошел onCreateView, то transition name будет установлено сразу.
     * Иначе, будет установлено после onViewCreated.
     *
     * @param transitionName новое имя или null (для скрытых фрагментов во viewpager)
     */
    public void setSharedElementTransitionName(@Nullable String transitionName) {
        mTransitionName = transitionName;
        if (mColorImageButton != null) {
            ViewCompat.setTransitionName(mColorImageButton, mTransitionName);
        }
    }

    /** Загружает данные в фрагмент из mFromItem.
     * Если mFromItem == null (создание нового элемента), не делает ничего.
     *
     * @param rootView корневой элемент фрагмента или null (если вызывается после onCreateView)
     *
     * @throws IllegalStateException если вызван до onCreateView и rootView == null
     */
    void reloadDataFromItem(@Nullable View rootView) {

        if (getView() != null) {
            rootView = getView();
        } else if (rootView == null) {
            throw new IllegalStateException("Error @ EditItemFragment.reloadDataFromItem(): rootView, getView == null");
        }

        if (mFromItem != null) {

            // Редактирование элемента
            final SimpleDateFormat dateFormatter = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            mLabelEditText.setText(mFromItem.getLabel());
            mDescriptionEditText.setText(mFromItem.getDescription());
            setColor(mFromItem.getColor());
            ((TextView) rootView.findViewById(R.id.tv_edititem_created_value))
                    .setText(dateFormatter.format(mFromItem.getDateCreated()));
            ((TextView) rootView.findViewById(R.id.tv_edititem_modified_value))
                    .setText(dateFormatter.format(mFromItem.getDateModified()));
            String imageUrl = mFromItem.getImageUrl();
            if (!imageUrl.isEmpty()) {
                mImageUrlEditText.setText(imageUrl);
                reloadImage();
            }

        }

    }

    /** Вызывает перезагрузку menu, устанавливает новый тулбар.
     *
     * @param rootView корневной элемент фрагмента или null (если вызывается после onCreateView)
     *
     * @throws IllegalStateException если вызван до onCreateView и rootView == null
     */
    void reloadMenu(@Nullable View rootView) {

        if (getView() != null) {
            rootView = getView();
        } else if (rootView == null) {
            throw new IllegalStateException("Error @ EditItemFragment.reloadDataFromItem(): rootView, getView == null");
        }

        if (mCallbacks != null) {
            // Инициализация Toolbar
            String actionBarTitle = mFromItem == null
                    ? getString(R.string.edititem_title_add) : getString(R.string.edititem_title_edit);
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_edititem);
            toolbar.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.icons));
            mCallbacks.setNewToolbar(toolbar, actionBarTitle);

        }

    }

    /** Показывает AlertDialog с текстом об имеющихся изменениях и кнопками "ОК И ОТМЕНА".
     *
     * @param onExitWithoutSaving действие, которое нужно выполнить в случае сброса изменений
     * @param onCancelExit действие, которое нужно выполнить в случае возвращения к редактированию
     */
    void showModifiedAlertWithOptions(@Nullable Runnable onExitWithoutSaving, @Nullable Runnable onCancelExit) {
        ActivityUtils.showAlertDialog(
                getContext(),
                getString(R.string.edititem_menu_back_question_title),
                getString(R.string.edititem_menu_back_question_text),
                onExitWithoutSaving,
                onCancelExit);
    }

}
