package com.example.mborzenkov.readlaterlist;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

public class EditItem extends AppCompatActivity {

    private int mChosenColor;
    private EditText mLabelEditText;
    private EditText mDescriptionEditText;
    private ImageButton mColorImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_edit_item);
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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ReadLaterItem.KEY_EXTRA) && intent.hasExtra(ReadLaterItem.KEY_UID)) {
            ReadLaterItem itemData = intent.getParcelableExtra(ReadLaterItem.KEY_EXTRA);
            mLabelEditText.setText(itemData.getLabel());
            mDescriptionEditText.setText(itemData.getDescription());
            mChosenColor = itemData.getColor();
        } else {
            // Создание новой
            mChosenColor = Color.RED;
            // TODO: Цвет по умолчанию в ресурсы
        }
        ((GradientDrawable) mColorImageButton.getBackground()).setColor(mChosenColor);
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
            // TODO: Фокус на поле и просьба заполнить
            return null;
        }
    }


    // TODO: В Delee установить ReadLaterItem в null
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


}
