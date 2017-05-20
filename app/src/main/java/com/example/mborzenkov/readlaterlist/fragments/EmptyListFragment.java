package com.example.mborzenkov.readlaterlist.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/** Фрагмент с онбордингом для пустого списка. */
public class EmptyListFragment extends Fragment {

    /* Этот фрагмент нужен, потому что в будущем онбординг может стать полноценным фрагментом.
     * Поэтому следует его сразу воспринимать как фрагмент, чтобы не переписывать потом с элемента на фрагмент.
     */

    /** TAG фрагмента для фрагмент менеджера. */
    private static final String TAG = "fragment_emptylist";

    /** Возвращает уже созданный ранее объект EmptyListFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     *
     * @return новый объект EmptyListFragment
     */
    public static EmptyListFragment getInstance(FragmentManager fragmentManager, @IdRes int containerId) {

        EmptyListFragment fragment = (EmptyListFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new EmptyListFragment();
            fragmentManager.beginTransaction()
                    .add(containerId, fragment, TAG).commit();
        }

        return fragment;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

}
