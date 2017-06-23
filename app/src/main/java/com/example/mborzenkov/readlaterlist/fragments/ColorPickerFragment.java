package com.example.mborzenkov.readlaterlist.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.adt.CustomColor;
import com.example.mborzenkov.readlaterlist.adt.CustomColor.Hsv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Fragment для выбора цвета с помощью палитры.
 * Использование:
 *      Для получения объекта всегда используйте getInstance.
 *      Для заполнения установки начального цвета, необходимо передать его в getInstance.
 *      Для получения результатов редактирования, необходимо, чтобы Activity, использующая фрагмент, реализовывала
 *          интерфейс ColorPickerCallbacks.
 */
public class ColorPickerFragment extends Fragment implements View.OnTouchListener, View.OnLongClickListener {

    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_colorpicker";

    // Ключи для Bundle.
    private static final String BUNDLE_DEFAULTCOLOR_KEY = "colorpicker_defaultcolor";

    // Ключи для savedInstanceState
    private static final String SAVEDINSTANCE_CHOSENCOLOR_KEY = "colorpicker_chosencolor";
    private static final String SAVEDINSTANCE_GRADIENTCOLORS_KEY = "colorpicker_gradientcolors";

    /** Задержка для проверки двойного клика. */
    private static final long QUALIFICATION_SPAN = 200;
    /** Минимальное время между вибрациями. */
    private static final int TIMEOUT_VIBRATE = 1000; // 1 сек
    /** Продолжительность вибрации. */
    private static final int VIBRATE_LENGTH = 50; // 0.05 сек

    /** Модификатор изменений HUE. */
    private static final int DIV_HUE_MODIFIER = 10;
    /** Модификатор изменений VAL. */
    private static final int DIV_VAL_MODIFIER = 500;
    /** Маска для затемнения градиента. */
    private static final int GRADIENT_FADE_MASK = 25;
    /** Четверть. */
    private static final int QUARTER = 4;


    /////////////////////////
    // Static

    /** Варианты границ градиента: Левая и Правая. */
    private enum GradientBorders { LEFT, RIGHT }

    /** Последняя случившаяся вибрация. */
    private static long sLastVibrate = 0;

    /** Возвращает уже созданный ранее объект ColorPickerFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу, не null
     * @param defaultColor цвет, который нужно установить при открытии фрагмента в формате CustomColor, не null
     *
     * @return новый объект ColorPickerFragment, не null
     *
     * @throws NullPointerException если любой из параметров == null
     */
    public static @NonNull ColorPickerFragment getInstance(@NonNull FragmentManager fragmentManager,
                                                           @NonNull CustomColor defaultColor) {

        ColorPickerFragment fragment = (ColorPickerFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new ColorPickerFragment();
        }

        Bundle args = new Bundle();
        args.putInt(BUNDLE_DEFAULTCOLOR_KEY, defaultColor.getColorRgb());
        fragment.setArguments(args);

        return fragment;

    }

    /** Интерфейс для оповещений о событиях во фрагменте. */
    public interface ColorPickerCallbacks extends BasicFragmentCallbacks {

        /** Возвращает массив любимых цветов.
         * Размер массива любимых цветов = максимальному возможному количеству.
         * Если цвет не задан, то он равен Color.TRANSPARENT
         *
         * @return список любимых цветов, не null
         */
        @NonNull int[] getFavoriteColors();

        /** Вызывается, когда пользователь желает соханить цвет.
         *
         * @param newColor цвет для сохранения в формате CustomColor, не null
         * @param position позиция любимого цвета, >= 0
         *
         * @throws IllegalArgumentException если position < 0
         * @throws NullPointerException если newColor == null
         */
        void saveFavoriteColor(@NonNull CustomColor newColor, @IntRange(from = 0) int position);

        /** Вызывается при завершении редактирования объекта и необходимости сохранения изменений.
         * Если ничего не изменено, onColorChosen не вызывается.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         *
         * @param newColor выбранный цвет, не null
         *
         * @throws NullPointerException если newColor == null
         */
        void onColorPicked(@NonNull CustomColor newColor);

        /** Вызывается при выходе без изменений.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         */
        void onEndPickingColor();

    }

    /** Возвращает новый Drawable для последующей установки в нем цвета.
     * Drawable уже может обладать случайным цветом.
     *
     * @return Drawable
     *
     * @throws NullPointerException если context == null
     */
    private static GradientDrawable newColorDrawable(@NonNull Context context) {
        return (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.circle_stroke);
    }

    /** Возвращает граничное значение от переданного цвета.
     *      В случае LEFT, возвращает цвет, с измененным HUE на - stepHue/2, но не менее минимального допустимого
     *      В случае RIGHT, возвращает цвет, с измененным HUE на + stepHue/2, но не более максимального допустимого
     *
     * @param color цвет в формате CustomColor, не null
     * @param border тип запрашиваемой границы: LEFT или RIGHT, не null
     * @param stepHue разница HUE между краями градиента LEFT и RIGHT, >= 1
     *
     * @return цвет в формате CustomColor, соответствующий граничному значению
     *
     * @throws IllegalArgumentException если stepHue < 1
     * @throws NullPointerException если color == null || border == null
     */
    private static CustomColor getBorder(@NonNull CustomColor color,
                                         @NonNull GradientBorders border,
                                         @IntRange(from = 1) int stepHue) {
        if (stepHue < 1) {
            throw new IllegalArgumentException("Error @ ColorPickerFragment.getBorder :: stepHue == " + stepHue);
        }
        switch (border) {
            case LEFT:
                return CustomColor.colorWithModifiedHsv(color, Hsv.HUE, -((float) stepHue / 2));
            case RIGHT:
                return CustomColor.colorWithModifiedHsv(color, Hsv.HUE, -((float) stepHue / 2));
            default:
                throw new RuntimeException(
                        "Error @ ColorPickerFragment.getBorder() :: border must be LEFT or RIGHT but was " + border);
        }
    }

    /////////////////////////
    // Поля объекта

    // Инвариант:
    //      mStepHue - шаг, на котором располгаются края градиентов, > 0
    //      mDivHue - значение, на которое изменяется HUE при перетягивании, >0
    //      mDivVal - значение, на которое изменяется VAL при перятягивании, >0
    //      mDefaultColor - главный цвет по умолчанию
    //      mStandardColors - список цветов в градиенте по умолчанию,
    //              не null, не пустой, каждый цвет отличается от следующего по HUE на mStepHue, каждый элемент float[3]
    //      mCurrentColors - список текущих цветов в градиенте,
    //              не null, не пустой, размер равен mStandardColors, каждый элемент float[3]
    //      mFavoriteColors - список любимых цветов, не null
    //      mChosenColor - текущий выбранный цвет, не null, размерность 3

    /** Скорость перетягивания по горизонтали. */
    private int mDivHue;
    /** Скорость перетягивания по вертикали. */
    private int mDivVal;

    /** Список цветов у элементов градиента по умолчанию. */
    private @NonNull List<CustomColor> mStandardColors = Collections.emptyList();
    /** Список текущих цветов у элементов градиента. */
    private @NonNull List<CustomColor> mCurrentColors = Collections.emptyList();
    /** Список любимых цветов в формате CustomColor. */
    private @NonNull List<CustomColor> mFavoriteColors = Collections.emptyList();
    /** Выбранный цвет. */
    private @NonNull CustomColor mChosenColor = CustomColor.getTransparent();

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable ColorPickerCallbacks mCallbacks = null;

    // Элементы layout и помощники
    private HorizontalScrollView mScrollView;
    private LinearLayout mColorsLinearLayout;
    private LinearLayout mFavLinearLayout;
    private TextView mRgbValueTextView;
    private TextView mHsvValueTextView;

    private Handler mHandler;
    private Vibrator mVibrator;
    // -

    // Вспомогательные переменные
    /** Признак режима редактирования. */
    private boolean editingMode = false;
    /** Признак двойного клика. */
    private boolean doubleClick = false;
    /** Последний элемент, на который кликнули. */
    private @Nullable ImageButton lastClickedElement = null;
    /** Предыдущее положение нажатия по X. */
    private int deltaX = 0;
    /** Предыдущее положение нажатия по Y. */
    private int deltaY = 0;


    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ColorPickerCallbacks) {
            mCallbacks = (ColorPickerCallbacks) context;
        }
    }

    /** {@inheritDoc}
     *
     * @throws IllegalStateException если объект создается без Bundle или не содержит BUNDLE_DEFAULTCOLOR_KEY,
     *              что значит, что он создется не через getInstance
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Error @ ColorPickerFragment.onCreate: Bundle == null");
        } else if (!args.containsKey(BUNDLE_DEFAULTCOLOR_KEY)) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.onCreate: !Bundle.containsKey(BUNDLE_DEFAULTCOLOR_KEY)");
        }

        /* Выбранный по умолчанию цвет в этом фрагменте. */
        mChosenColor = new CustomColor(args.getInt(BUNDLE_DEFAULTCOLOR_KEY));

        // Скорость перетягивания зависит от дисплея
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double) width / (double) dm.xdpi;
        double hi = (double) height / (double) dm.ydpi;
        mDivHue = (int) (wi * DIV_HUE_MODIFIER);
        mDivVal = (int) (hi * DIV_VAL_MODIFIER);

        // Контекст, максимальное количество Favorites, общее число элементов
        final Context context = getContext();
        final Resources resources = getResources();
        final int numberOfElements = resources.getInteger(R.integer.colorpicker_circles);

        // Массив Favorites, список цветов на поле градиента и шаг между цветами
        if (mCallbacks != null) {
            mFavoriteColors = new ArrayList<>();
            for (int color : mCallbacks.getFavoriteColors()) {
                mFavoriteColors.add(new CustomColor(color));
            }
        }

        mStandardColors = new ArrayList<>(numberOfElements);
        mCurrentColors = new ArrayList<>(numberOfElements);
        final int step = (int) ((Hsv.HUE.to()) / (float) numberOfElements);
        final CustomColor baseColor = CustomColor.colorWithModifiedHsv(
                new CustomColor(ContextCompat.getColor(context, R.color.gradient_start)), Hsv.HUE, Hsv.HUE.from());

        /* Рассчитывает mStandardColors.
         * Каждый элемент mStandardColors - это середина между левым краем градиента и правым.
         * Элементы начинаются с colorGradientStart + (mStepHue / 2) и каждый следующий равен предыдущему + mStepHue.
         */
        CustomColor curColor = CustomColor.colorWithModifiedHsv(baseColor, Hsv.HUE, (float) step / 2);
        for (int i = 0; i < numberOfElements; i++) {
            mStandardColors.add(curColor);
            mCurrentColors.add(curColor);
            curColor = CustomColor.colorWithModifiedHsv(curColor, Hsv.HUE, step);
        }

    }

    /** {@inheritDoc}
     *
     * @throws IllegalStateException если при восстановлении из savedInstanceState были найдены ключи
     *              SAVEDINSTANCE_CHOSENCOLOR_KEY и SAVEDINSTANCE_GRADIENTCOLORS_KEY, но не удалось восстановить из них
     *              значения
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_colorpicker, container, false);

        // Инициализация объектов layout
        mScrollView         = (HorizontalScrollView) rootView.findViewById(R.id.horizontalScrollView);
        mColorsLinearLayout = (LinearLayout) rootView.findViewById(R.id.linearLayout_main);
        mFavLinearLayout    = (LinearLayout) rootView.findViewById(R.id.linearlayout_filterdrawer_favorites);
        mRgbValueTextView   = (TextView) rootView.findViewById(R.id.textView_RGB_value);
        mHsvValueTextView   = (TextView) rootView.findViewById(R.id.textView_HSV_value);

        // Инициализация хелперов
        mHandler = new Handler();
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        // Заполненяем mChosenColor и mCurrentColors из savedInstanceState
        if ((savedInstanceState != null) && savedInstanceState.containsKey(SAVEDINSTANCE_CHOSENCOLOR_KEY)
                && savedInstanceState.containsKey(SAVEDINSTANCE_GRADIENTCOLORS_KEY)) {

            final int numberOfElements = mCurrentColors.size();

            // Восстановление из SavedInstanceState
            final int savedChosenColor = savedInstanceState.getInt(SAVEDINSTANCE_CHOSENCOLOR_KEY, Color.TRANSPARENT);
            if (savedChosenColor != Color.TRANSPARENT) {
                mChosenColor = new CustomColor(savedChosenColor);
            } else {
                throw new IllegalStateException(
                        "Error @ ColorPickerFragment.onCreateView: savedInstanceState contain all keys, but "
                                + "savedChosenColor is invalid");
            }

            final List<Integer> savedGradientColors =
                    savedInstanceState.getIntegerArrayList(SAVEDINSTANCE_GRADIENTCOLORS_KEY);
            if ((savedGradientColors != null) && (savedGradientColors.size() == numberOfElements)) {
                for (int i = 0; i < numberOfElements; i++) {
                    int color = savedGradientColors.get(i);
                    mCurrentColors.set(i, new CustomColor(color));
                }
            } else {
                throw new IllegalStateException(
                        "Error @ ColorPickerFragment.onCreateView: savedInstanceState contain all keys, but "
                                + "savedGradientColors is invalid");
            }

        }

        Context context = getContext();

        // Ставим цвет у центрального цветного элемента и у приложения
        GradientDrawable mainElementDrawable = newColorDrawable(context);
        mainElementDrawable.setColor(mChosenColor.getColorRgb());
        View mainElement = rootView.findViewById(R.id.imageButton_chosen);
        mainElement.setBackground(mainElementDrawable);
        mainElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickOnChosenElement(v);
            }
        });
        changeMainColor(mChosenColor, false);

        // Shared element
        ViewCompat.setTransitionName(mainElement, MainActivity.SHARED_ELEMENT_COLOR_TRANSITION_NAME);

        // Инициализируем все цветные объекты
        inflateFavoriteDrawables(inflater, context);
        inflateGradientDrawables(inflater, context);

        // Объекты и действия, имеющие смысл только при наличии колбеков
        if (mCallbacks != null) {

            // Инициализация тулбара
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_colorpicker);
            toolbar.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.icons));
            mCallbacks.setNewToolbar(toolbar, getString(R.string.title_activity_colorpicker));

        }

        // Меню нет
        setHasOptionsMenu(true);

        return rootView;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVEDINSTANCE_CHOSENCOLOR_KEY, mChosenColor.getColorRgb());
        ArrayList<Integer> mainGradientElements = new ArrayList<>();
        for (CustomColor elementColor : mCurrentColors) {
            mainGradientElements.add(elementColor.getColorRgb());
        }
        outState.putIntegerArrayList(SAVEDINSTANCE_GRADIENTCOLORS_KEY, mainGradientElements);
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
    // Вспомогательные методы для создания объекта

    /** Создает все элементы в layout Favorites.
     * Очищает mFavLinearLayout и добавляет mFavoriteColors.length элементов в него.
     * Устанавливает каждому элементу цвет в соответствии с mFavoriteColors.
     * Каждому добавленному элементу устанавливает Tag с его порядковым номером.
     *
     * @param inflater инфлейтер для создания view
     * @param context контекст для обращения к ресурсам
     *
     * @throws IllegalStateException если mFavoriteColors == null
     * @throws IllegalStateException если mFavLinearLayout == null
     * @throws android.view.InflateException если не удалось добавить view в mFavLinearLayout
     * @throws NullPointerException если inflater == null || context == null
     *
     */
    private void inflateFavoriteDrawables(@NonNull LayoutInflater inflater,
                                          @NonNull Context context) {

        if (mFavLinearLayout == null) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateFavoriteDrawables: mFavLinearLayout == null");
        }

        mFavLinearLayout.removeAllViews();

        for (int i = 0, maxFavorites = mFavoriteColors.size(); i < maxFavorites; i++) {

            GradientDrawable favoritesDrawable = newColorDrawable(context);
            favoritesDrawable.setColor(mFavoriteColors.get(i).getColorRgb());
            View favElement = inflater.inflate(R.layout.content_colorpicker_favorites, mFavLinearLayout, false);
            View elementView = favElement.findViewById(R.id.imageButton_favorite_color);
            elementView.setOnLongClickListener(this);
            elementView.setBackground(favoritesDrawable);
            elementView.setTag(i);
            elementView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickOnFavElement(v);
                }
            });
            mFavLinearLayout.addView(favElement);

        }

    }

    /** Создает все элементы в layout mColorsLinearLayout.
     * Очищает mColorsLinearLayout и добавляет mCurrentColors.size() элементов в него.
     * Устанавливает каждому элементу цвет в соответствии с mCurrentColors.
     * Каждому добавленному элементу станавливает Tag в соответствии с его порядовым номером.
     * Устанавливает градиент в mColorsLinearLayout в соответствии с mStandardColors.
     *
     * @param inflater инфлейтер для создания view
     * @param context контекст для обращения к ресурсам
     *
     * @throws IllegalStateException если mCurrentColors.size() == 0
     * @throws IllegalStateException если mStandardColors.size() == 0
     * @throws IllegalStateException если mColorsLinearLayout == null
     * @throws IllegalStateException если mStepHue <= 0
     * @throws android.view.InflateException если не удалось добавить view в mColorsLinearLayout
     * @throws NullPointerException если inflater == null || context == null
     */
    private void inflateGradientDrawables(@NonNull LayoutInflater inflater,
                                          @NonNull Context context) {

        final int numberOfElements = mCurrentColors.size();

        if (mCurrentColors.isEmpty()) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mCurrentColors is empty");
        } else if (mStandardColors.isEmpty()) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mStandardColors is empty");
        } else if (mColorsLinearLayout == null) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mColorsLinearLayout == null");
        }

        mColorsLinearLayout.removeAllViews();

        // Эти два массива нужны для создания мультиградиента
        final int[] mArrayOfGradient = new int[numberOfElements + 1];
        final float[] arrayOfPositions = new float[numberOfElements + 1];

        // Шаг
        final int step = (int) ((Hsv.HUE.to()) / (float) numberOfElements);

        // Считаем начальный цвет
        mArrayOfGradient[0] = getBorder(mStandardColors.get(0), GradientBorders.LEFT, step).getColorRgb();
        arrayOfPositions[0] = 0;

        // Заполняем цветными элементами поле градиента
        for (int i = 0; i < numberOfElements; i++) {

            // Запоминаем правую границу градиента и ее положение
            mArrayOfGradient[i + 1] =
                    getBorder(mStandardColors.get(i), GradientBorders.RIGHT, step).getColorRgb();
            arrayOfPositions[i + 1] = (float) i / (float) numberOfElements;

            // Создаем цветной элемент внутри градиента
            GradientDrawable gradientChildDrawable = newColorDrawable(context);
            gradientChildDrawable.setColor(mCurrentColors.get(i).getColorRgb());

            // Создаем View, устанавливаем ему бэкграунд в виде цветного элемента
            View gradientChild = inflater.inflate(R.layout.content_colorpicker_circle, mColorsLinearLayout, false);
            View gradientChildImageButton = gradientChild.findViewById(R.id.imageButton_colored_square);
            // Устанавливаем ему Listener'ы для действий с ним
            gradientChildImageButton.setOnLongClickListener(this);
            gradientChildImageButton.setOnTouchListener(this);
            gradientChildImageButton.setBackground(gradientChildDrawable);
            // Простой способ потом понимать какой элемент какой - установить таги
            gradientChildImageButton.setTag(i);
            mColorsLinearLayout.addView(gradientChild);

        }

        // Создаем мультиградиент и устанавливаем его
        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, 0, width, height,
                        mArrayOfGradient,
                        arrayOfPositions,
                        Shader.TileMode.REPEAT);
            }
        };
        PaintDrawable perfectGradient = new PaintDrawable();
        perfectGradient.setShape(new RectShape());
        perfectGradient.setShaderFactory(shaderFactory);
        mColorsLinearLayout.setBackground(perfectGradient);

    }


    /////////////////////////
    // Колбеки меню

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                if (mCallbacks != null) {
                    mCallbacks.onEndPickingColor();
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /////////////////////////
    // Колбеки onTouchListener

    @Override
    @SuppressLint("ClickableViewAccessibility") // в ACTION_UP вызывается performClick
    public boolean onTouch(View view, MotionEvent event) {
        if (view.getId() != R.id.imageButton_colored_square) {
            return false;
        }

        final ImageButton element = (ImageButton) view;
        // Получаем позицию эвента
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // При нажатии, запоминаем куда нажали
                deltaX = X;
                deltaY = Y;
                return false;
            case MotionEvent.ACTION_UP:
                // При отпускании, если был режим редактирования, отключаем его; иначе обрабатываем клик
                if (editingMode) {
                    editingMode = false;
                    mScrollView.requestDisallowInterceptTouchEvent(false);
                    changeMainColor(mChosenColor, false);
                    mColorsLinearLayout.getBackground().clearColorFilter();
                } else if (doubleClick && element.equals(lastClickedElement)) {
                    reverseColor(element);
                    doubleClick = false;
                } else {
                    doubleClick = true;
                    lastClickedElement = element;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (doubleClick) {
                                element.performClick();
                                clickOnElement(element);
                                doubleClick = false;
                            }

                        }
                    }, QUALIFICATION_SPAN);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                // Передвижение работает только в режиме редактирования, меняем цвета
                if (editingMode) {
                    float hue = ((float) (X - deltaX)) / mDivHue;
                    float val = -((float) (Y - deltaY)) / mDivVal;
                    updateColor(element, hue, val);
                    deltaX = X;
                    deltaY = Y;
                }
                mFavLinearLayout.invalidate();
                return false;
            default:
                return false;
        }
    }


    /////////////////////////
    // Колбеки onLongClickListener

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton_colored_square:
                if (!editingMode && (v.getTag() != null)) {
                    editingMode = true;
                    mScrollView.requestDisallowInterceptTouchEvent(true);
                    int position = (Integer) v.getTag();
                    changeMainColor(mCurrentColors.get(position), false);
                    // Затеняем градиент
                    mColorsLinearLayout.getBackground().setColorFilter(
                            Color.argb(GRADIENT_FADE_MASK, 0, 0, 0), PorterDuff.Mode.DARKEN);
                    vibrate();
                }
                break;
            case R.id.imageButton_favorite_color:
                if (v.getTag() != null) {
                    if (mCallbacks != null) {
                        int position = (Integer) v.getTag();
                        setFavoriteColor(position, mChosenColor);
                        mCallbacks.saveFavoriteColor(mChosenColor, position);
                        vibrate();
                    }
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }


    /////////////////////////
    // Обработчики нажатий

    /** Обработчик нажатия на элемент Favorites.
     * При вызове меняет текущий выбранный цвет на цвет выбранного элемента Fav, надписей вокруг него и строки меню
     *
     * @param view элемент Favorites
     *
     * @throws NullPointerException если view == null
     */
    private void clickOnFavElement(@NonNull View view) {
        if (view.getTag() != null) {
            final int position = (Integer) view.getTag();
            CustomColor colorAtPosition = mFavoriteColors.get(position);
            if (!colorAtPosition.isTransparent()) {
                changeMainColor(colorAtPosition, true);
            }
        }
    }

    /** Обработчик нажатия на текущий выбранный цвет.
     * При вызове закрывает Activity и возвращает цвет
     *
     * @param view ImageButton выбранный цвет
     *
     * @throws NullPointerException если view == null
     */
    private void clickOnChosenElement(@NonNull View view) {
        if (view.getId() == R.id.imageButton_chosen) {
            if (mCallbacks != null) {
                mCallbacks.onColorPicked(mChosenColor);
            }
        }
    }

    /** Обработчик нажатия на элемент градиента.
     * При вызове меняет текущий выбранный цвет на цвет элемента, надписей вокруг него, строки меню и Favorites
     *
     * @param element элемент градиента
     *
     * @throws NullPointerException если element == null
     */
    private void clickOnElement(@NonNull ImageButton element) {
        if (element.getTag() != null) {
            final int position = (Integer) element.getTag();
            changeMainColor(mCurrentColors.get(position), true);
        }
    }


    /////////////////////////
    // Методы при заершении редактирования

    /** Вызывается, когда нажата кнопка назад.
     * Управление полностью передается фрагменту. Фрагмент должен обработать нажатие самостоятельно.
     */
    public void onBackPressed() {
        if (mCallbacks != null) {
            mCallbacks.onEndPickingColor();
        }
    }


    /////////////////////////
    // Методы для управления цветами

    /** Меняет текущий выбранный цвет, надписей вокруг него и строки меню на переданный newColor.
     *
     * @param newColor Новый цвет
     * @param saveAsChosen Признак, нужно ли сохранить цвет
     *
     * @throws NullPointerException если newColor == null
     */
    private void changeMainColor(@NonNull CustomColor newColor, boolean saveAsChosen) {
        int color = newColor.getColorRgb();
        if (saveAsChosen) {
            mChosenColor = newColor;
        }
        if (getView() != null) {
            ((GradientDrawable) getView().findViewById(R.id.imageButton_chosen).getBackground()).setColor(color);
        }
        mRgbValueTextView.setText(String.format(Locale.US, "%d, %d, %d",
                Color.red(color), Color.green(color), Color.blue(color)));
        mHsvValueTextView.setText(String.format(Locale.US, "%.2f, %.2f, %.2f",
                newColor.getHsvAttr(Hsv.HUE), newColor.getHsvAttr(Hsv.SAT), newColor.getHsvAttr(Hsv.VAL)));
    }

    /** Обрабатывает изменения цвета в элементе.
     *
     * @param element элемент
     * @param hue Изменение HUE от стандартного значения
     * @param val Изменение VAL от стандартного значения
     *
     * @throws NullPointerException если element == null
     */
    private void updateColor(@NonNull ImageButton element, float hue, float val) {

        final int position = (Integer) element.getTag();

        final float leftBorderHue  = position == 0
                ? Hsv.HUE.from() : mStandardColors.get(position - 1).getHsvAttr(Hsv.HUE);
        final float rightBorderHue = position == (mStandardColors.size() - 1)
                ? Hsv.HUE.to() : mStandardColors.get(position + 1).getHsvAttr(Hsv.HUE);

        final float standardVal = mStandardColors.get(position).getHsvAttr(Hsv.VAL);
        final float leftBorderVal = Math.max(standardVal - (standardVal / QUARTER), Hsv.VAL.from());
        final float rightBorderVal = Math.min(standardVal + (standardVal / QUARTER), Hsv.VAL.to());

        final CustomColor currentColorAtPosition  = mCurrentColors.get(position);
        float modifiedHue = currentColorAtPosition.getHsvAttr(Hsv.HUE) + hue;
        float modifiedVal = currentColorAtPosition.getHsvAttr(Hsv.VAL) + val;

        if (modifiedHue <= leftBorderHue) {
            modifiedHue = leftBorderHue;
            vibrate();
        } else if (modifiedHue >= rightBorderHue) {
            modifiedHue = rightBorderHue;
            vibrate();
        }

        if (modifiedVal <= leftBorderVal) {
            modifiedVal = leftBorderVal;
            vibrate();
        } else if (modifiedVal >= rightBorderVal) {
            modifiedVal = rightBorderVal;
            vibrate();
        }

        final CustomColor newColor =
                CustomColor.colorWithModifiedHsv(
                        CustomColor.colorWithModifiedHsv(
                                currentColorAtPosition,
                                Hsv.HUE,
                                modifiedHue - currentColorAtPosition.getHsvAttr(Hsv.HUE)),
                        Hsv.VAL,
                        modifiedVal - currentColorAtPosition.getHsvAttr(Hsv.VAL));

        mCurrentColors.set(position, newColor);
        setElementColor(element, newColor);
        changeMainColor(newColor, false);

    }

    /** Возвращает цвет элемента к цвету по умолчанию.
     *
     * @param element элемент
     *
     * @throws NullPointerException если element == null
     */
    private void reverseColor(@NonNull ImageButton element) {
        final int position = (Integer) element.getTag();
        CustomColor standardColor = mStandardColors.get(position);
        mCurrentColors.set(position, standardColor);
        setElementColor(element, standardColor);
    }

    /** Меняет цвет у элемента.
     *
     * @param element элемент, у которого нужно изменить цвет
     * @param newColor новый цвет
     *
     * @throws NullPointerException если elemen == null || newColor == null
     */
    private void setElementColor(@NonNull ImageButton element, @NonNull CustomColor newColor) {
        ((GradientDrawable) element.getBackground()).setColor(newColor.getColorRgb());
    }

    /** Запоминает цвет в выбранном элементе Favorites.
     *
     * @param position Позиция элемента Favorites, в котором нужно запомнить цвет
     * @param color Цвет, который нужно запомнить
     *
     * @throws IllegalArgumentException если position < 0 || position >= mFavoriteColors.size()
     * @throws NullPointerException если color == null
     */
    private void setFavoriteColor(@IntRange(from = 0) int position, @NonNull CustomColor color) {
        if ((position < 0) || (position >= mFavoriteColors.size())) {
            throw new IllegalArgumentException(
                    "Error @ ColorPickerFragment.setFavoriteColor :: position not in range == " + position);
        }
        View favElement = mFavLinearLayout.getChildAt(position).findViewById(R.id.imageButton_favorite_color);
        ((GradientDrawable) favElement.getBackground()).setColor(color.getColorRgb());
        mFavoriteColors.set(position, color);
    }


    /////////////////////////
    // Вспомогательные методы

    /** Заставляет телефон вибрировать. */
    private void vibrate() {
        if (System.currentTimeMillis() - sLastVibrate > TIMEOUT_VIBRATE) {
            mVibrator.vibrate(VIBRATE_LENGTH);
            sLastVibrate = System.currentTimeMillis();
        }
    }

}
