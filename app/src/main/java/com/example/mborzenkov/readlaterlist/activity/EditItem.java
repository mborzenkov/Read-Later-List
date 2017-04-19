package com.example.mborzenkov.readlaterlist.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;

public class EditItem extends AppCompatActivity implements View.OnClickListener {

    private static final int ITEM_EDIT_COLOR_REQUEST = 11;

    private int mChosenColor;
    private EditText mLabelEditText;
    private EditText mDescriptionEditText;
    private ImageButton mColorImageButton;

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

        mLabelEditText = (EditText) findViewById(R.id.et_edit_item_title);
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
            fab.setImageResource(R.drawable.ic_add_24dp);
        } else {
            // Создание новой
            getSupportActionBar().setTitle(getString(R.string.edititem_title_add));
            fab.setImageResource(R.drawable.ic_edit_24dp);
            mChosenColor = ContextCompat.getColor(this, R.color.item_default_color);
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
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_remove) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete this entry?")
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
