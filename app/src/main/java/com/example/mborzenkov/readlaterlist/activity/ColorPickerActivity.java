package com.example.mborzenkov.readlaterlist.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Activity для выбора цвета с помощью палитры
 * Использование:
 *      Для установки выбранного цвета при открытии необходимо передать int
 *              в Intent с ключем ColorPickerActivity.CHOSEN_KEY
 *      При выборе цвета (нажатии на выбранный цвет) возвращает его как int
 *              в Intent с ключем  ColorPickerActivity.CHOSEN_KEY
 */
public class ColorPickerActivity extends AppCompatActivity implements View.OnTouchListener, View.OnLongClickListener {

    // Объявляем все переменные
    /** Константа для использования в качестве ключа при сохранении массива Favorites. */
    private static final String FAVORITES_KEY = "com.example.mborzenkov.colorpicker.favorites";
    /** Константа для использования в качестве ключа при сохранении выбранного цвета. */
    public static final String CHOSEN_KEY = "com.example.mborzenkov.colorpicker.chosen";

    /** Задержка для проверки двойного клика. */
    private static final long QUALIFICATION_SPAN = 200;
    /** Минимальное время между вибрациями. */
    private static final int TIMEOUT_VIBRATE = 1000; // 1 сек
    /** Продолжительность вибрации. */
    private static final int VIBRATE_LENGTH = 50; // 0.05 сек

    /** Последняя случившаяся вибрация. */
    private static long sLastVibrate = 0;

    // Элементы layout и помощники
    private HorizontalScrollView mScrollView;
    private LinearLayout mColorsLinearLayout;
    private LinearLayout mFavLinearLayout;
    private TextView mRgbValueTextView;
    private TextView mHsvValueTextView;

    private SharedPreferences mSharedPreferences;
    private Handler mHandler;
    private Vibrator mVibrator;
    // -

    /** Шаг, на котором будут располагаться края мультиградиента, квадрат находится на STEP_HUE/2 от края градиента. */
    private int mStepHue;
    /** Скорость перетягивания по горизонтали. */
    private int mDivHue;
    /** Скорость перетягивания по вертикали. */
    private int mDivVal;

    /** Список цветов у квадратов по умолчанию в формате HSV. */
    private List<float[]> mSquareStandardColorsHsv = new ArrayList<>();
    /** Список текущих цветов у квадратов в формате HSV. */
    @SuppressWarnings("CanBeFinal") // Потому что он не может быть final, потому что добавляются элементы
    private List<float[]> mSquareColorsHsv = new ArrayList<>();
    /** Список любимых цветов в формате Color. */
    private int[] mFavoriteColors;
    /** Выбранный цвет в формате HSV. */
    private float[] chosenColorHsv = new float[3];

    /** Признак режима редактирования. */
    private boolean editingMode = false;
    /** Признак двойного клика. */
    private boolean doubleClick = false;
    /** Последний квадрат, на который кликнули. */
    private ImageButton lastClickedSquare = null;
    /** Предыдущее положение нажатия по X. */
    private int deltaX = 0;
    /** Предыдущее положение нажатия по Y. */
    private int deltaY = 0;

    /** Считает шаг для HUE между краями мультиградиента.
     * @param startColor Начальный цвет
     * @param endColor Конечный цвет
     *                 Требуется, чтобы HUE в endColor был > HUE в startColor
     * @param numberOfSquares Количество квадратов
     * @return Шаг step*numberOfSquares ~= hue(endColor) - hue(startColor)
     */
    private static int countStep(int startColor, int endColor, int numberOfSquares) {
        float[] startColorHsv = new float[3];
        float[] endColorHsv = new float[3];
        Color.colorToHSV(startColor, startColorHsv);
        Color.colorToHSV(endColor, endColorHsv);

        return (int) ((endColorHsv[0] - startColorHsv[0]) / (float) numberOfSquares);
    }

    /** Копирует цвет в формате HSV.
     *
     * @param colorFrom Цвет в формате HSV,
     */
    private static float[] copyOfColor(@NonNull float[] colorFrom) {
        return Arrays.copyOf(colorFrom, 3);
    }

    /** Возвращает новый Drawable квадратик с прозрачным цветом.
     *
     * @return Квадратик Drawable
     */
    private GradientDrawable getSquareDrawable(Context context) {
        return (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.circle_stroke);
    }

    /** Заставляет телефон вибрировать.
     *
     */
    private void vibrate() {
        if (System.currentTimeMillis() - sLastVibrate > TIMEOUT_VIBRATE) {
            mVibrator.vibrate(VIBRATE_LENGTH);
            sLastVibrate = System.currentTimeMillis();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colorpicker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_colorpicker);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Context applicationContext = getApplicationContext();
        LayoutInflater layoutInflater = getLayoutInflater();
        GradientDrawable squareDrawable = getSquareDrawable(applicationContext);
        squareDrawable.setColor(Color.TRANSPARENT);
        Intent fromIntent = getIntent();

        // Инициализация
        mScrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        mColorsLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_main);
        mFavLinearLayout = (LinearLayout) findViewById(R.id.linearlayout_filterdrawer_favorites);
        mRgbValueTextView = (TextView) findViewById(R.id.textView_RGB_value);
        mHsvValueTextView = (TextView) findViewById(R.id.textView_HSV_value);
        mSharedPreferences = getSharedPreferences(FAVORITES_KEY, Context.MODE_PRIVATE);
        mHandler = new Handler();
        mVibrator = (Vibrator) applicationContext.getSystemService(VIBRATOR_SERVICE);

        final int numberOfSquares = getResources().getInteger(R.integer.colorpicker_circles);
        final int colorGradientStart = ContextCompat.getColor(applicationContext, R.color.gradient_start);
        final int colorGradientEnd = ContextCompat.getColor(applicationContext, R.color.gradient_end);
        mStepHue = countStep(colorGradientStart, colorGradientEnd, numberOfSquares);

        // Скорость перетягивания зависит от дисплея
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double)width / (double)dm.xdpi;
        double hi = (double)height / (double)dm.ydpi;
        mDivHue = (int) (wi * 10);
        mDivVal = (int) (hi * 500);

        // Инициализируем массив Favorites
        final int maxFavorites = getResources().getInteger(R.integer.colorpicker_favorites);
        mFavoriteColors = new int[maxFavorites];

        // Эти два массива нужны для создания мультиградиента
        final int[] mArrayOfGradient = new int[numberOfSquares + 1];
        final float[] arrayOfPositions = new float[numberOfSquares + 1];

        // Создаем Favorites квадратики
        for (int i = 0; i < maxFavorites; i++) {

            squareDrawable = getSquareDrawable(applicationContext);
            View favSquare = layoutInflater.inflate(R.layout.content_colorpicker_favorites, mFavLinearLayout, false);
            View squareButton = favSquare.findViewById(R.id.imageButton_favorite_color);
            squareButton.setOnLongClickListener(this);
            squareButton.setBackground(squareDrawable);
            squareButton.setTag(i);
            mFavLinearLayout.addView(favSquare);

        }

        if (savedInstanceState == null) {
            for (int i = 0; i < maxFavorites; i++) {
                int savedColor = mSharedPreferences.getInt(String.valueOf(i), Color.TRANSPARENT);
                setFavoriteColor(i, savedColor);
            }
        }

        // Считаем начальный цвет
        int curColor = colorGradientStart;
        float[] curColorHsv = new float[3];
        Color.colorToHSV(curColor, curColorHsv);
        mArrayOfGradient[0] = curColor;
        arrayOfPositions[0] = 0;

        // Заполняем квадратиками поле
        for (int i = 0; i < numberOfSquares; i++) {

            // Создаем Drawable квадратик и запоминаем его цвет в mSquareColorsHsv
            curColorHsv[0] += mStepHue / 2;
            curColor = Color.HSVToColor(curColorHsv);
            squareDrawable = getSquareDrawable(applicationContext);
            squareDrawable.setColor(curColor);
            mSquareColorsHsv.add(copyOfColor(curColorHsv));
            curColorHsv[0] += mStepHue / 2;

            // Запоминаем правую границу градиента и ее положение
            curColor = Color.HSVToColor(curColorHsv);
            mArrayOfGradient[i + 1] = curColor;
            arrayOfPositions[i + 1] = (float) i / (float) numberOfSquares;

            // Создаем View квадратик
            View square = layoutInflater.inflate(R.layout.content_colorpicker_circle, mColorsLinearLayout, false);
            View squareButton = square.findViewById(R.id.imageButton_colored_square);
            // Устанавливаем ему Listener'ы для действий с ним
            squareButton.setOnLongClickListener(this);
            squareButton.setOnTouchListener(this);
            squareButton.setBackground(squareDrawable);
            // Простой способ потом понимать какой квадратик какой - установить таги
            squareButton.setTag(i);
            mColorsLinearLayout.addView(square);

            Color.colorToHSV(curColor, curColorHsv);

        }

        // Ставим цвет у главного квадратика и у приложения
        squareDrawable = getSquareDrawable(applicationContext);
        findViewById(R.id.imageButton_chosen).setBackground(squareDrawable);
        if (savedInstanceState == null) {
            if (fromIntent.hasExtra(CHOSEN_KEY)) {
                Color.colorToHSV(fromIntent.getIntExtra(CHOSEN_KEY, Color.TRANSPARENT), chosenColorHsv);
            } else {
                chosenColorHsv = copyOfColor(mSquareColorsHsv.get(0));
            }
            changeMainColor(chosenColorHsv, true);
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

        // Запоминаем все начальные значения
        mSquareStandardColorsHsv = new ArrayList<>();
        for (float[] val : mSquareColorsHsv) {
            mSquareStandardColorsHsv.add(copyOfColor(val));
        }

    }

    /** Возвращает выбранный цвет и закрывает Activity.
     * @param color Выбранный цвет
     */
    private void closeWithResult(int color) {
        Intent result = new Intent();
        result.putExtra(CHOSEN_KEY, color);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int[] favoritesFromState = savedInstanceState.getIntArray(FAVORITES_KEY);
            if (favoritesFromState != null) {
                for (int i = 0; i < favoritesFromState.length; i++) {
                    if (favoritesFromState[i] != 0) {
                        setFavoriteColor(i, favoritesFromState[i]);
                    }
                }
                mFavoriteColors = Arrays.copyOf(favoritesFromState, favoritesFromState.length);
            }
            float[] chosenColorFromState = savedInstanceState.getFloatArray(CHOSEN_KEY);
            if (chosenColorFromState != null) {
                changeMainColor(chosenColorFromState, true);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(FAVORITES_KEY, mFavoriteColors);
        outState.putFloatArray(CHOSEN_KEY, chosenColorHsv);
    }

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
                    changeMainColor(chosenColorHsv, false);
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
                    mColorsLinearLayout.getBackground().setColorFilter(Color.argb(25, 0, 0, 0), PorterDuff.Mode.DARKEN);
                    vibrate();
                }
                break;
            case R.id.imageButton_favorite_color:
                if (v.getTag() != null) {
                    int position = (Integer) v.getTag();
                    int colorFavorite = Color.HSVToColor(chosenColorHsv);
                    setFavoriteColor(position, colorFavorite);
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(String.valueOf(position), colorFavorite);
                    editor.apply();
                    vibrate();
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    /** Обработчик нажатия на Favorites квадратик.
     * При вызове меняет цвет у главного квадратика, надписей вокруг него и строки меню
     * @param view Квадратик
     */
    public void clickOnFavSquare(@NonNull View view) {
        if (view.getTag() != null) {
            final int position = (Integer) view.getTag();
            if (mFavoriteColors[position] != 0) {
                float[] colorOfSquare = new float[3];
                Color.colorToHSV(mFavoriteColors[position], colorOfSquare);
                changeMainColor(colorOfSquare, true);
            }
        }
    }

    /** Обработчик нажатия на выбранный квадратик.
     * При вызове закрывает Activity и возвращает цвет
     * @param view ImageButton выбранный цвет
     */
    public void clickOnChosenSquare(@NonNull View view) {
        if (view.getId() == R.id.imageButton_chosen) {
            closeWithResult(Color.HSVToColor(chosenColorHsv));
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

    /** Меняет цвет у основного квадратика, надписей вокруг него и строки меню на переданный colorHSV.
     *
     * @param colorHsv Цвет в HSV, float[] размерностью 3
     * @param saveAsChosen Признак, нужно ли сохранить цвет
     */
    private void changeMainColor(@NonNull float[] colorHsv, boolean saveAsChosen) {
        if (saveAsChosen) {
            chosenColorHsv = copyOfColor(colorHsv);
        }
        int color = Color.HSVToColor(colorHsv);
        ((GradientDrawable) findViewById(R.id.imageButton_chosen).getBackground()).setColor(color);
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
        rightBorderHue = Math.min(rightBorderHue + mStepHue, 360);
        float topVal = Math.min(standardColorHsv[2] + (standardColorHsv[2] / 4), 1);
        float bottomVal = Math.max(standardColorHsv[2] - (standardColorHsv[2] / 4), 0);

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
}
