package com.example.mborzenkov.readlaterlist.fragments;

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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
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

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** Ошибка при нарушении инварианта. */
    private static final String ERROR_INVARIANT_FAIL = "ColorPickerFragment invariant break: %s";

    /** Задержка для проверки двойного клика. */
    private static final long QUALIFICATION_SPAN = 200;
    /** Минимальное время между вибрациями. */
    private static final int TIMEOUT_VIBRATE = 1000; // 1 сек
    /** Продолжительность вибрации. */
    private static final int VIBRATE_LENGTH = 50; // 0.05 сек

    /** Размерность Color HSV. */
    private static final int HSV_SIZE = 3;
    /** Модификатор изменений HUE. */
    private static final int DIV_HUE_MODIFIER = 10;
    /** Модификатор изменений VAL. */
    private static final int DIV_VAL_MODIFIER = 500;
    /** Маска для затемнения градиента. */
    private static final int GRADIENT_FADE_MASK = 25;
    /** Максимальное возможное значение HUE. */
    private static final int HUE_MAX = 360;
    /** Четверть. */
    private static final int QUARTER = 4;


    /////////////////////////
    // Static

    /** Последняя случившаяся вибрация. */
    private static long sLastVibrate = 0;

    /** Возвращает уже созданный ранее объект ColorPickerFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @param defaultColor цвет, который нужно установить при открытии фрагмента в формате sRGB
     *
     * @return новый объект EditItemFragment
     */
    public static ColorPickerFragment getInstance(FragmentManager fragmentManager, int defaultColor) {

        ColorPickerFragment fragment = (ColorPickerFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new ColorPickerFragment();
        }

        Bundle args = new Bundle();
        args.putInt(BUNDLE_DEFAULTCOLOR_KEY, defaultColor);
        fragment.setArguments(args);

        return fragment;

    }

    /** Интерфейс для оповещений о событиях во фрагменте. */
    public interface ColorPickerCallbacks extends BasicFragmentCallbacks {

        /** Вызывается при завершении редактирования объекта и необходимости сохранения изменений.
         * Если ничего не изменено, onColorChosen не вызывается.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         *
         * @param newColor выбранный цвет
         */
        void onColorPicked(int newColor);

        /** Вызывается при выходе без изменений.
         * При этом Fragment не закрывается, получатель колбека должен закрыть его самостоятельно.
         */
        void onEndPickingColor();

    }

    /** Считает шаг для HUE между краями мультиградиента.
     * @param startColor Начальный цвет
     * @param endColor Конечный цвет
     *                 Требуется, чтобы HUE в endColor был > HUE в startColor
     * @param numberOfSquares Количество квадратов
     * @return Шаг step*numberOfSquares ~= hue(endColor) - hue(startColor)
     */
    private static int countStep(int startColor, int endColor, int numberOfSquares) {
        float[] startColorHsv = new float[HSV_SIZE];
        float[] endColorHsv = new float[HSV_SIZE];
        Color.colorToHSV(startColor, startColorHsv);
        Color.colorToHSV(endColor, endColorHsv);

        return (int) ((endColorHsv[0] - startColorHsv[0]) / (float) numberOfSquares);
    }

    /** Копирует цвет в формате HSV.
     *
     * @param colorFrom Цвет в формате HSV,
     */
    private static float[] copyOfColor(@NonNull float[] colorFrom) {
        return Arrays.copyOf(colorFrom, HSV_SIZE);
    }

    /** Возвращает новый Drawable для последующей установки в нем цвета.
     * Drawable уже может обладать случайным цветом.
     *
     * @return Drawable
     */
    private static GradientDrawable newColorDrawable(Context context) {
        return (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.circle_stroke);
    }

    /** Варианты границ градиента: Левая и Правая.
     * Перечисление для чистоты кода и простоты понимания, вместо true/false.
     */
    private enum GradientBorders { LEFT, RIGHT }

    /** Возвращает граничное значение от переданного цвета.
     *      В случае LEFT, возвращает цвет, с измененным HUE на - stepHue/2;
     *      В случае RIGHT, возвращает цвет, с измененным HUE на + stepHue/2
     *
     * @param color цвет в формате HSV
     * @param border тип запрашиваемой границы: LEFT или RIGHT
     * @param stepHue разница HUE между краями градиента LEFT и RIGHT
     *
     * @return цвет в формате sRGB, соответствующий граничному значению
     */
    private static int getBorder(@NonNull float[] color, @NonNull GradientBorders border, int stepHue) {
        float[] tmpColor = copyOfColor(color);
        switch (border) {
            case LEFT:
                tmpColor[0] -= (stepHue / 2);
                break;
            case RIGHT:
                tmpColor[0] += (stepHue / 2);
                break;
            default:
                break;
        }
        return Color.HSVToColor(tmpColor);
    }

    /////////////////////////
    // Поля объекта

    // Инвариант:
    //      mStepHue - шаг, на котором располгаются края градиентов, > 0
    //      mDivHue - значение, на которое изменяется HUE при перетягивании, >0
    //      mDivVal - значение, на которое изменяется VAL при перятягивании, >0
    //      mDefaultColor - главный цвет по умолчанию
    //      mSquareStandardColorsHsv - список цветов в градиенте по умолчанию,
    //              не null, не пустой, каждый цвет отличается от следующего по HUE на mStepHue, каждый элемент float[3]
    //      mSquareColorsHsv - список текущих цветов в градиенте,
    //              не null, не пустой, размер равен mSquareStandardColorsHsv, каждый элемент float[3]
    //      mFavoriteColors - список любимых цветов, не null
    //      mChosenColorHsv - текущий выбранный цвет, не null, размерность 3

    /** Шаг, на котором будут располагаться края мультиградиента, квадрат находится на STEP_HUE/2 от края градиента. */
    private int mStepHue;
    /** Скорость перетягивания по горизонтали. */
    private int mDivHue;
    /** Скорость перетягивания по вертикали. */
    private int mDivVal;

    /** Список цветов у квадратов по умолчанию в формате HSV. */
    private @NonNull List<float[]> mSquareStandardColorsHsv = Collections.emptyList();
    /** Список текущих цветов у квадратов в формате HSV. */
    private @NonNull List<float[]> mSquareColorsHsv = Collections.emptyList();
    /** Список любимых цветов в формате Color. */
    private @NonNull int[] mFavoriteColors = new int[0];
    /** Выбранный цвет в формате HSV. */
    private @NonNull @Size(HSV_SIZE) float[] mChosenColorHsv = new float[HSV_SIZE];

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
    /** Последний квадрат, на который кликнули. */
    private @Nullable ImageButton lastClickedSquare = null;
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

        /* Цвет по умолчанию в формате sRGB в этом фрагменте. */
        int defaultColor = args.getInt(BUNDLE_DEFAULTCOLOR_KEY);

        // Скорость перетягивания зависит от дисплея
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double) width / (double) dm.xdpi;
        double hi = (double) height / (double) dm.ydpi;
        mDivHue = (int) (wi * DIV_HUE_MODIFIER);
        mDivVal = (int) (hi * DIV_VAL_MODIFIER);

        // Контекст, максимальное количество Favorites, общее число квадратиков
        Context context = getContext();
        Resources resources = getResources();
        final int numberOfSquares = resources.getInteger(R.integer.colorpicker_circles);

        // Массив Favorites, список цветов на поле градиента и шаг между цветами
        mFavoriteColors = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(context, null);
        mSquareStandardColorsHsv = new ArrayList<>(numberOfSquares);
        mSquareColorsHsv = new ArrayList<>(numberOfSquares);
        final int colorGradientStart = ContextCompat.getColor(context, R.color.gradient_start);
        final int colorGradientEnd = ContextCompat.getColor(context, R.color.gradient_end);
        mStepHue = countStep(colorGradientStart, colorGradientEnd, numberOfSquares);


        /* Рассчитывает mSquareStandardColorsHsv.
         * Каждый элемент mSquareStandardColorsHsv - это середина между левым краем градиента и правым.
         * Элементы начинаются с colorGradientStart + (mStepHue / 2) и каждый следующий равен предыдущему + mStepHue.
         */
        {
            if (BigInteger.valueOf(colorGradientStart)
                    .add(
                            BigInteger.valueOf(mStepHue)
                                    .multiply(BigInteger.valueOf(numberOfSquares)))
                    .compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1) {
                throw new IllegalArgumentException(
                        "Error @ ColorPickerFragment.calculateGradient: colorGradientStart + "
                                + "(mStepHue * mSquareStandardColorsHsv.size()) > Integer.MAX_VALUE");
            } else if (mStepHue <= 0) {
                throw new IllegalStateException(
                        "Error @ ColorPickerFragment.calculateGradient: mStepHue == " + mStepHue);
            }

            float[] transparentColorHsv = new float[HSV_SIZE];
            Color.colorToHSV(Color.TRANSPARENT, transparentColorHsv);
            float[] curColorHsv = new float[HSV_SIZE];
            Color.colorToHSV(colorGradientStart, curColorHsv);
            curColorHsv[0] += mStepHue / 2;
            for (int i = 0; i < numberOfSquares; i++) {
                mSquareStandardColorsHsv.add(copyOfColor(curColorHsv));
                mSquareColorsHsv.add(copyOfColor(curColorHsv));
                curColorHsv[0] += mStepHue;
            }

            float[] defaultColorHsv = new float[HSV_SIZE];
            Color.colorToHSV(defaultColor, defaultColorHsv);
            mChosenColorHsv = copyOfColor(defaultColorHsv);

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
        mScrollView = (HorizontalScrollView) rootView.findViewById(R.id.horizontalScrollView);
        mColorsLinearLayout = (LinearLayout) rootView.findViewById(R.id.linearLayout_main);
        mFavLinearLayout = (LinearLayout) rootView.findViewById(R.id.linearlayout_filterdrawer_favorites);
        mRgbValueTextView = (TextView) rootView.findViewById(R.id.textView_RGB_value);
        mHsvValueTextView = (TextView) rootView.findViewById(R.id.textView_HSV_value);

        // Инициализация хелперов
        mHandler = new Handler();
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        // Заполненяем mSquareColorsHsv
        if ((savedInstanceState != null) && savedInstanceState.containsKey(SAVEDINSTANCE_CHOSENCOLOR_KEY)
                && savedInstanceState.containsKey(SAVEDINSTANCE_GRADIENTCOLORS_KEY)) {

            final int numberOfSquares = mSquareColorsHsv.size();

            // Восстановление из SavedInstanceState
            float[] savedChosenColor = savedInstanceState.getFloatArray(SAVEDINSTANCE_CHOSENCOLOR_KEY);
            if ((savedChosenColor != null) && (savedChosenColor.length == HSV_SIZE)) {
                mChosenColorHsv = savedChosenColor;
            } else {
                throw new IllegalStateException(
                        "Error @ ColorPickerFragment.onCreateView: savedInstanceState contain all keys, but "
                                + "savedChosenColor is invalid");
            }
            List<Integer> savedGradientColors =
                    savedInstanceState.getIntegerArrayList(SAVEDINSTANCE_GRADIENTCOLORS_KEY);
            if ((savedGradientColors != null) && (savedGradientColors.size() == numberOfSquares)) {
                for (int i = 0; i < numberOfSquares; i++) {
                    int color = savedGradientColors.get(i);
                    float[] hsvColor = new float[HSV_SIZE];
                    Color.colorToHSV(color, hsvColor);
                    mSquareColorsHsv.set(i, hsvColor);
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
        mainElementDrawable.setColor(Color.HSVToColor(mChosenColorHsv));
        View mainElement = rootView.findViewById(R.id.imageButton_chosen);
        mainElement.setBackground(mainElementDrawable);
        mainElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickOnChosenSquare(v);
            }
        });
        changeMainColor(mChosenColorHsv, false);

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
        outState.putFloatArray(SAVEDINSTANCE_CHOSENCOLOR_KEY, mChosenColorHsv);
        ArrayList<Integer> mainGradientElements = new ArrayList<>();
        for (float[] elementColor : mSquareColorsHsv) {
            mainGradientElements.add(Color.HSVToColor(elementColor));
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
     *
     */
    private void inflateFavoriteDrawables(@NonNull LayoutInflater inflater,
                                          @NonNull Context context) {

        if (mFavLinearLayout == null) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateFavoriteDrawables: mFavLinearLayout == null");
        }

        mFavLinearLayout.removeAllViews();

        for (int i = 0, maxFavorites = mFavoriteColors.length; i < maxFavorites; i++) {

            GradientDrawable favoritesDrawable = newColorDrawable(context);
            favoritesDrawable.setColor(mFavoriteColors[i]);
            View favSquare = inflater.inflate(R.layout.content_colorpicker_favorites, mFavLinearLayout, false);
            View squareButton = favSquare.findViewById(R.id.imageButton_favorite_color);
            squareButton.setOnLongClickListener(this);
            squareButton.setBackground(favoritesDrawable);
            squareButton.setTag(i);
            squareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickOnFavSquare(v);
                }
            });
            mFavLinearLayout.addView(favSquare);

        }

    }

    /** Создает все элементы в layout mColorsLinearLayout.
     * Очищает mColorsLinearLayout и добавляет mSquareColorsHsv.size() элементов в него.
     * Устанавливает каждому элементу цвет в соответствии с mSquareColorsHsv.
     * Каждому добавленному элементу станавливает Tag в соответствии с его порядовым номером.
     * Устанавливает градиент в mColorsLinearLayout в соответствии с mSquareStandardColorsHsv.
     *
     * @param inflater инфлейтер для создания view
     * @param context контекст для обращения к ресурсам
     *
     * @throws IllegalStateException если mSquareColorsHsv.size() == 0
     * @throws IllegalStateException если mSquareStandardColorsHsv.size() == 0
     * @throws IllegalStateException если mColorsLinearLayout == null
     * @throws IllegalStateException если mStepHue <= 0
     * @throws android.view.InflateException если не удалось добавить view в mColorsLinearLayout
     */
    private void inflateGradientDrawables(@NonNull LayoutInflater inflater,
                                          @NonNull Context context) {

        final int numberOfSquares = mSquareColorsHsv.size();

        if (mSquareColorsHsv.isEmpty()) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mSquareColorsHsv is empty");
        } else if (mSquareStandardColorsHsv.isEmpty()) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mSquareStandardColorsHsv is empty");
        } else if (mColorsLinearLayout == null) {
            throw new IllegalStateException(
                    "Error @ ColorPickerFragment.inflateGradientDrawables: mColorsLinearLayout == null");
        } else if (mStepHue <= 0) {
            throw new IllegalStateException(
                "Error @ ColorPickerFragment.inflateGradientDrawables: mStepHue == " + mStepHue);
        }

        mColorsLinearLayout.removeAllViews();

        // Эти два массива нужны для создания мультиградиента
        final int[] mArrayOfGradient = new int[numberOfSquares + 1];
        final float[] arrayOfPositions = new float[numberOfSquares + 1];

        // Считаем начальный цвет
        mArrayOfGradient[0] = getBorder(mSquareStandardColorsHsv.get(0), GradientBorders.LEFT, mStepHue);
        arrayOfPositions[0] = 0;

        // Заполняем цветными элементами поле градиента
        for (int i = 0; i < numberOfSquares; i++) {

            // Запоминаем правую границу градиента и ее положение
            mArrayOfGradient[i + 1] = getBorder(mSquareStandardColorsHsv.get(i), GradientBorders.RIGHT, mStepHue);
            arrayOfPositions[i + 1] = (float) i / (float) numberOfSquares;

            // Создаем цветной элемент внутри градиента
            GradientDrawable gradientChildDrawable = newColorDrawable(context);
            gradientChildDrawable.setColor(Color.HSVToColor(mSquareColorsHsv.get(i)));

            // Создаем View, устанавливаем ему бэкграунд в виде цветного элемента
            View gradientChild = inflater.inflate(R.layout.content_colorpicker_circle, mColorsLinearLayout, false);
            View gradientChildImageButton = gradientChild.findViewById(R.id.imageButton_colored_square);
            // Устанавливаем ему Listener'ы для действий с ним
            gradientChildImageButton.setOnLongClickListener(this);
            gradientChildImageButton.setOnTouchListener(this);
            gradientChildImageButton.setBackground(gradientChildDrawable);
            // Простой способ потом понимать какой квадратик какой - установить таги
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
    public boolean onTouch(View view, MotionEvent event) {
        if (view.getId() != R.id.imageButton_colored_square) {
            return false;
        }

        ImageButton square = (ImageButton) view;
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
                    changeMainColor(mChosenColorHsv, false);
                    mColorsLinearLayout.getBackground().clearColorFilter();
                } else if (doubleClick && square.equals(lastClickedSquare)) {
                    reverseColor(square);
                    doubleClick = false;
                } else {
                    doubleClick = true;
                    lastClickedSquare = square;
                    mHandler.postDelayed(new HandleClick(square), QUALIFICATION_SPAN);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                // Передвижение работает только в режиме редактирования, меняем цвета
                if (editingMode) {
                    float hue = ((float) (X - deltaX)) / mDivHue;
                    float val = ((float) (Y - deltaY)) / mDivVal;
                    updateColor(square, hue, val);
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
                    changeMainColor(mSquareColorsHsv.get(position), false);
                    // Затеняем градиент
                    mColorsLinearLayout.getBackground().setColorFilter(
                            Color.argb(GRADIENT_FADE_MASK, 0, 0, 0), PorterDuff.Mode.DARKEN);
                    vibrate();
                }
                break;
            case R.id.imageButton_favorite_color:
                if (v.getTag() != null) {
                    int position = (Integer) v.getTag();
                    int colorFavorite = Color.HSVToColor(mChosenColorHsv);
                    setFavoriteColor(position, colorFavorite);
                    FavoriteColorsUtils.saveFavoriteColor(getContext(), null, colorFavorite, position);
                    vibrate();
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

    /** Обработчик нажатия на Favorites квадратик.
     * При вызове меняет цвет у главного квадратика, надписей вокруг него и строки меню
     * @param view Квадратик
     */
    private void clickOnFavSquare(@NonNull View view) {
        if (view.getTag() != null) {
            final int position = (Integer) view.getTag();
            if (mFavoriteColors[position] != 0) {
                float[] colorOfSquare = new float[HSV_SIZE];
                Color.colorToHSV(mFavoriteColors[position], colorOfSquare);
                changeMainColor(colorOfSquare, true);
            }
        }
    }

    /** Обработчик нажатия на выбранный квадратик.
     * При вызове закрывает Activity и возвращает цвет
     * @param view ImageButton выбранный цвет
     */
    private void clickOnChosenSquare(@NonNull View view) {
        if (view.getId() == R.id.imageButton_chosen) {
            if (mCallbacks != null) {
                mCallbacks.onColorPicked(Color.HSVToColor(mChosenColorHsv));
            }
        }
    }

    /** Класс для обработки двойного клика. */
    private class HandleClick implements Runnable {

        final ImageButton view;

        HandleClick(ImageButton view) {
            this.view = view;
        }

        public void run() {
            if (doubleClick) {
                clickOnSquare(view);
                doubleClick = false;
            }
        }

    }

    /** Обработчик нажатия на квадратик.
     * При вызове меняет цвет у главного квадратика, надписей вокруг него, строки меню и Favorites
     * @param square Квадратик
     */
    private void clickOnSquare(@NonNull ImageButton square) {
        if (square.getTag() != null) {
            final int position = (Integer) square.getTag();
            float[] colorOfSquare = mSquareColorsHsv.get(position);
            changeMainColor(colorOfSquare, true);
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

    /** Меняет цвет у основного квадратика, надписей вокруг него и строки меню на переданный colorHSV.
     *
     * @param colorHsv Цвет в HSV, float[] размерностью 3
     * @param saveAsChosen Признак, нужно ли сохранить цвет
     */
    private void changeMainColor(@NonNull @Size(HSV_SIZE) float[] colorHsv, boolean saveAsChosen) {
        if (saveAsChosen) {
            mChosenColorHsv = copyOfColor(colorHsv);
        }
        int color = Color.HSVToColor(colorHsv);
        if (getView() != null) {
            ((GradientDrawable) getView().findViewById(R.id.imageButton_chosen).getBackground()).setColor(color);
        }
        mRgbValueTextView.setText(String.format(Locale.US, "%d, %d, %d",
                Color.red(color), Color.green(color), Color.blue(color)));
        mHsvValueTextView.setText(String.format(Locale.US, "%.2f, %.2f, %.2f", colorHsv[0], colorHsv[1], colorHsv[2]));
    }

    /** Обрабатывает изменения цвета в квадратике.
     *
     * @param square Квадратик
     * @param hue Изменение HUE от стандартного значения
     * @param val Изменение VAL от стандартного значения
     */
    private void updateColor(@NonNull ImageButton square, float hue, float val) {

        final int position = (Integer) square.getTag();
        float[] standardColorHsv = mSquareStandardColorsHsv.get(position);
        float[] currentColorHsv = mSquareColorsHsv.get(position);
        float leftBorderHue = standardColorHsv[0];
        float rightBorderHue = standardColorHsv[0];
        leftBorderHue = Math.max(leftBorderHue - mStepHue, 0);
        rightBorderHue = Math.min(rightBorderHue + mStepHue, HUE_MAX);
        float topVal = Math.min(standardColorHsv[2] + (standardColorHsv[2] / QUARTER), 1);
        float bottomVal = Math.max(standardColorHsv[2] - (standardColorHsv[2] / QUARTER), 0);

        float changedHue = currentColorHsv[0] + hue;
        float changedVal = currentColorHsv[2] - val;

        if (changedHue < leftBorderHue) {
            changedHue = leftBorderHue;
            vibrate();
        } else if (changedHue > rightBorderHue) {
            changedHue = rightBorderHue;
            vibrate();
        }

        if (changedVal < bottomVal) {
            changedVal = bottomVal;
            vibrate();
        } else if (changedVal > topVal) {
            changedVal = topVal;
            vibrate();
        }

        currentColorHsv[0] = changedHue;
        currentColorHsv[2] = changedVal;


        setSquareColor(square, currentColorHsv);
        changeMainColor(currentColorHsv, false);
    }

    /** Возвращает цвет квадратика по умолчанию.
     *
     * @param square Квадратик
     */
    private void reverseColor(@NonNull ImageButton square) {
        final int position = (Integer) square.getTag();
        float[] standardColorHsv = mSquareStandardColorsHsv.get(position);
        float[] currentColorHsv;
        currentColorHsv = copyOfColor(standardColorHsv);
        mSquareColorsHsv.set(position, currentColorHsv);
        setSquareColor(square, currentColorHsv);
    }

    /** Меняет цвет у указанного квадратика на указанный.
     *
     * @param square Квадратик, у которого нужно изменить цвет
     * @param colorHsv Цвет в HSV
     */
    private void setSquareColor(@NonNull ImageButton square, @NonNull float[] colorHsv) {
        int dynamicColor = Color.HSVToColor(colorHsv);
        ((GradientDrawable) square.getBackground()).setColor(dynamicColor);
    }

    /** Запоминает цвет в выбранном любимом квадратике.
     *
     * @param position Позиция квадратика, в котором нужно запомнить цвет
     * @param color Цвет, который нужно запомнить
     */
    private void setFavoriteColor(int position, int color) {
        View favSquare = mFavLinearLayout.getChildAt(position).findViewById(R.id.imageButton_favorite_color);
        ((GradientDrawable) favSquare.getBackground()).setColor(color);
        mFavoriteColors[position] = color;
    }


    /////////////////////////
    // Вспомогательные методы

    /** Заставляет телефон вибрировать.
     *
     */
    private void vibrate() {
        if (System.currentTimeMillis() - sLastVibrate > TIMEOUT_VIBRATE) {
            mVibrator.vibrate(VIBRATE_LENGTH);
            sLastVibrate = System.currentTimeMillis();
        }
    }

}
