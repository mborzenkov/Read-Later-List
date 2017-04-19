package com.example.mborzenkov.readlaterlist.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;

public class EditItem extends AppCompatActivity implements View.OnClickListener {

    private static final int ITEM_EDIT_COLOR_REQUEST = 11;

    private int mChosenColor;
    private EditText mLabelEditText;
    private TextInputLayout mLabelInputLayout;
    private EditText mDescriptionEditText;
    private ImageButton mColorImageButton;
    private boolean mNewItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_edit_item);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_edit_item_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReadLaterItem resultData = packInputData();
                if (resultData != null) {
                    sendResult(resultData);
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLabelEditText = (EditText) findViewById(R.id.et_edit_item_label);
        mLabelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { mLabelInputLayout.setError(null); }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) { }
        });
        mLabelInputLayout = (TextInputLayout) findViewById(R.id.til_edit_item_label);
        mDescriptionEditText = (EditText) findViewById(R.id.et_edit_item_description);
        mColorImageButton = (ImageButton) findViewById(R.id.ib_edit_item_color);
        mColorImageButton.setOnClickListener(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ReadLaterItem.KEY_EXTRA) && intent.hasExtra(ReadLaterItem.KEY_UID)) {
            ReadLaterItem itemData = intent.getParcelableExtra(ReadLaterItem.KEY_EXTRA);
            mLabelEditText.setText(itemData.getLabel());
            mDescriptionEditText.setText(itemData.getDescription());
            mChosenColor = itemData.getColor();
            getSupportActionBar().setTitle(getString(R.string.edititem_title_edit));
            fab.setImageResource(R.drawable.ic_edit_24dp);
            mNewItem = false;
        } else {
            // Создание новой
            mNewItem = true;
            getSupportActionBar().setTitle(getString(R.string.edititem_title_add));
            fab.setImageResource(R.drawable.ic_add_24dp);
            mChosenColor = ContextCompat.getColor(this, R.color.item_default_color);
            if(mLabelEditText.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
        updateChosenColor();
    }

    private void sendResult(ReadLaterItem resultData) {
        Intent resultIntent = getIntent();
        resultIntent.putExtra(ReadLaterItem.KEY_EXTRA, resultData);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private ReadLaterItem packInputData() {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNewItem) {
            getMenuInflater().inflate(R.menu.menu_edit_item, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.edititem_action_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.edititem_menu_delete_question_title))
                    .setMessage(getString(R.string.edititem_menu_delete_question_text))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sendResult(null);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ib_edit_item_color) {
            Intent colorPicker = new Intent(EditItem.this, ColorPicker.class);
            colorPicker.putExtra(ColorPicker.CHOSEN_KEY, mChosenColor);
            startActivityForResult(colorPicker, ITEM_EDIT_COLOR_REQUEST);
        }
    }

    private void updateChosenColor() {
        ((GradientDrawable) mColorImageButton.getBackground()).setColor(mChosenColor);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == ITEM_EDIT_COLOR_REQUEST && data != null && data.hasExtra(ColorPicker.CHOSEN_KEY)) {
            mChosenColor = data.getIntExtra(ColorPicker.CHOSEN_KEY, Color.TRANSPARENT);
            updateChosenColor();
        }
    }
}
