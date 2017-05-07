package com.example.mborzenkov.readlaterlist.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;

/**
 * Activity для редактирования элемента MainListActivity
 * Использование:
 *      Чтобы заполнить Activity данными, передать в Intent объект ReadLaterItem с ключем ReadLaterItem.KEY_EXTRA
 *      При успешном редактировании возвращает новый объект ReadLaterItem в Intent под ключем ReadLaterItem.KEY_EXTRA
 *      При выборе удаления элемента, возвращает null в Intent под ключем ReadLaterItem.KEY_EXTRA
 */
public class EditItemActivity extends AppCompatActivity implements View.OnClickListener {

    /** ID для открытия ColorPickerActivity на редактирование цвета. */
    private static final int ITEM_EDIT_COLOR_REQUEST = 11;

    /** Текущий выбранный цвет в формате sRGB. */
    private int mChosenColor;
    /** Признаки - создается новый объект или редактируется имеющийся. */
    private enum EditModes { NEW, EDIT }
    /** Текущий режим. */
    private EditModes mMode = EditModes.NEW;
    /** Признак наличия изменений. */
    private boolean mModified = false;

    // Объекты layout
    private TextInputEditText mLabelEditText;
    private TextInputLayout mLabelInputLayout;
    private TextInputEditText mDescriptionEditText;
    private ImageButton mColorImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edititem);

        // Инициализация ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_edit_item);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_edit_item_save);
        fab.setOnClickListener(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Инициализация объектов layout
        mLabelEditText = (TextInputEditText) findViewById(R.id.et_edit_item_label);
        mLabelInputLayout = (TextInputLayout) findViewById(R.id.til_edit_item_label);
        mDescriptionEditText = (TextInputEditText) findViewById(R.id.et_edit_item_description);
        mColorImageButton = (ImageButton) findViewById(R.id.ib_edit_item_color);
        mColorImageButton.setOnClickListener(this);

        // Чтение данных из Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ReadLaterItemParcelable.KEY_EXTRA)) {
            // В Intent были переданы данные об объекте, записываем их в соответствующие поля
            ReadLaterItem itemData =
                    ((ReadLaterItemParcelable) intent.getParcelableExtra(ReadLaterItemParcelable.KEY_EXTRA)).getItem();
            mLabelEditText.setText(itemData.getLabel());
            mDescriptionEditText.setText(itemData.getDescription());
            mChosenColor = itemData.getColor();
            getSupportActionBar().setTitle(getString(R.string.edititem_title_edit));
            fab.setImageResource(R.drawable.ic_edit_24dp);
            mMode = EditModes.EDIT;
        } else {
            // В Intent не переданы данные или что-то пошло не так, в любом случае это новый объект
            // (возможно если что-то идет не так следует вызывать exception)
            mMode = EditModes.NEW;
            getSupportActionBar().setTitle(getString(R.string.edititem_title_add));
            fab.setImageResource(R.drawable.ic_add_24dp);
            mChosenColor = ContextCompat.getColor(this, R.color.item_default_color);
            // Устанавливаем фокус и открываем клавиатуру на редактирование Label, чтобы все было красиво и удобно
            if (mLabelEditText.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
        mLabelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mLabelInputLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                mModified = true;
            }
        });
        mDescriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                mModified = true;
            }
        });
        updateChosenColor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edititem, menu);
        if (mMode == EditModes.NEW) {
            // Кнопка удаления нужна только для редактируемых
            menu.removeItem(R.id.edititem_action_delete);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.edititem_action_delete:
                ActivityUtils.showAlertDialog(EditItemActivity.this,
                    getString(R.string.edititem_menu_delete_question_title),
                    getString(R.string.edititem_menu_delete_question_text),
                    () -> sendResult(null),
                    null);
                return true;
            case R.id.edititem_action_save:
                finishEditing();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.ib_edit_item_color:
                if (view.getId() == R.id.ib_edit_item_color) {
                    // Обработчик нажатий на иконку выбранного цвета, открывает ColorPickerActivity
                    Intent colorPicker = new Intent(EditItemActivity.this, ColorPickerActivity.class);
                    colorPicker.putExtra(ColorPickerActivity.CHOSEN_KEY, mChosenColor);
                    startActivityForResult(colorPicker, ITEM_EDIT_COLOR_REQUEST);
                }
                break;
            case R.id.fab_edit_item_save:
                finishEditing();
                break;
            default:
                break;
        }
    }

    /** Выполняет сохранение. */
    private void finishEditing() {
        // packInputData возвращает null, если что-то не так
        ReadLaterItem resultData = packInputData();
        if (resultData != null) {
            sendResult(resultData);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Принимает данные из ColorPickerActivity, сохраняет их
        if (resultCode == RESULT_OK && requestCode == ITEM_EDIT_COLOR_REQUEST
                && data != null && data.hasExtra(ColorPickerActivity.CHOSEN_KEY)) {
            int newColor = data.getIntExtra(ColorPickerActivity.CHOSEN_KEY, Color.TRANSPARENT);
            if (mChosenColor != newColor) {
                mChosenColor = newColor;
                updateChosenColor();
                mModified = true;
            }
        }
    }

    /** Устанавливает цвет ImageButton на mChosenColor. */
    private void updateChosenColor() {
        ((GradientDrawable) mColorImageButton.getBackground()).setColor(mChosenColor);
    }

    /**
     * Возвращает данные, добавляя объект ReadLaterItem в Intent
     * Объект добавляется под ключем ReadLaterItem.KEY_EXTRA
     * Если в Intent, открывшем Activity, был ключ ReadLaterItem.KEY_UID, то он добавляется к возвращаемому
     * @param resultData Объект для передачи
     */
    private void sendResult(ReadLaterItem resultData) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, resultData == null
                ? null : new ReadLaterItemParcelable(resultData));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /** Превращает данные формы в объект ReadLaterItem.
     *
     * @return Объект ReadLaterItem или null, если не удалось превратить данные в объект
     *      Например, если не заполнен Label
     */
    private @Nullable ReadLaterItem packInputData() {
        String label = mLabelEditText.getText().toString();
        String description = mDescriptionEditText.getText().toString();
        if (!label.trim().isEmpty()) {
            return new ReadLaterItem(label, description, mChosenColor);
        } else {
            mLabelInputLayout.setError(getString(R.string.edititem_error_title_empty));
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mModified) {
            ActivityUtils.showAlertDialog(EditItemActivity.this,
                    getString(R.string.edititem_menu_back_question_title),
                    getString(R.string.edititem_menu_back_question_text),
                    super::onBackPressed,
                    null);
        } else {
            super.onBackPressed();
        }
    }
}
