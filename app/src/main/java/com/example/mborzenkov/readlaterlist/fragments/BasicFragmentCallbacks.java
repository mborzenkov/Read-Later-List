package com.example.mborzenkov.readlaterlist.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

/** Стандартные колбеки для фрагментов. */
public interface BasicFragmentCallbacks {

    /** Вызывается, когда фрагменту нужно установить ActionBar.
     *
     * @param toolbar новый toolbar
     * @param title заголовок для ActionBar
     */
    void setNewToolbar(@NonNull Toolbar toolbar, @NonNull String title);

    /** Определяет, запущена ли сейчас какая-нибудь длительная операция. */
    boolean isLongTaskActive();

}
