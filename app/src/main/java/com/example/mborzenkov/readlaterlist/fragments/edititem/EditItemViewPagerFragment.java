package com.example.mborzenkov.readlaterlist.fragments.edititem;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;

/** Фрагмент для ViewPager с фрагментами EditItemFragment.
 * Использование:
 *      Для получения объекта всегда используйте getInstance.
 *      Для заполнения фрагмента данными, необходимо передать в getInstance объект ReadLaterItem и его itemLocalId.
 *      Фрагменты EditItemFragment во ViewPager для колбеков используют Activity, к которой они привязаны в onDetach.
 *      Для получения результатов редактирования, необходимо, чтобы Activity, использующая фрагмент, реализовывала
 *          интерфейс EditItemCallbacks.
 */
public class EditItemViewPagerFragment extends Fragment
        implements EditItemFragmentActions {

    // TODO: [ViewPager] Несколько фрагментов
    // Записать в JDoc класса и getInstance, не забыть переопределять mCurrentItem и mCurrentItemLocalId
    // TODO: [ViewPager] Табы и свайпы


    /////////////////////////
    // Константы

    public static final String TAG = "fragment_edititem_viewpager";


    /////////////////////////
    // PagerAdapter

    /** Адаптер для управления фрагментами EditItemFragment. */
    private class EditItemPagerAdapter extends FragmentStatePagerAdapter {

        private FragmentManager mFragmentManager;

        private EditItemPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            mFragmentManager = fragmentManager;
        }

        @Override
        public Fragment getItem(int position) {
            EditItemFragment fragment =
                    EditItemFragment.getInstance(mFragmentManager, mCurrentItem, mCurrentItemLocalId);
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if ((position == mViewPager.getCurrentItem()) && (obj instanceof EditItemFragment)) {
                mCurrentFragment = (EditItemFragment) obj;
                if (tmpColor != null) {
                    mCurrentFragment.setColor(tmpColor);
                    tmpColor = null;
                }
            }
            return obj;
        }

        @Override
        public int getCount() {
            return 1;
        }

    }


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект EditItemViewPagerFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     * Параметры item и itemLocalId используются для установки открываемого фрагмента.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @param item объект для редактирования или null, если создание нового элемента
     * @param itemLocalId внутренний идентификатор объекта или UID_EMPTY, если создание нового элемента
     *
     * @return новый объект EditItemViewPagerFragment
     *
     * @throws IllegalArgumentException если itemLocalId < UID_EMPTY
     * @throws IllegalArgumentException если item == null и itemLocalId != -UID_EMPTY
     */
    public static EditItemViewPagerFragment getInstance(FragmentManager fragmentManager,
                                                        @Nullable ReadLaterItem item,
                                                        @IntRange(from = UID_EMPTY) int itemLocalId) {

        if ((itemLocalId < UID_EMPTY) || ((item == null) && (itemLocalId != UID_EMPTY))) {
            throw new IllegalArgumentException(
                    String.format("Erorr @ EditItemViewPagerFragment.getInstance. itemLocalId: %s, item: %s",
                            itemLocalId, item));
        }

        EditItemViewPagerFragment fragment = (EditItemViewPagerFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new EditItemViewPagerFragment();
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
    //      mCurrentFragment - текущий фрагмент. Не null, если уже прошел instantiateItem у адаптера.
    //      mCurrentItem - текущий редактируемый элемент. Null, если создается новый объект, иначе не null
    //      mCurrentItemLocalId - внутренний идентификатор элемента. UID_EMPTY, если создается новый объект, иначе >= 0

    /** Текущий фрагмент. */
    private @Nullable EditItemFragment mCurrentFragment = null;
    /** Текущий редактируемый элемент. */
    private @Nullable ReadLaterItem mCurrentItem = null;
    /** Внутренний идентификатор текущего редактируемого элемента. */
    private @IntRange(from = UID_EMPTY) int mCurrentItemLocalId = UID_EMPTY;

    // Элементы layout
    private ViewPager mViewPager;

    /** Поле для сохранения цвета при переходе из ColorPicker в EditItemFragment.
     * Необходимо из-за промежутка между закрытием ColorPickerFragment и вызовом instantiateView в PagerAdapter.
     * Если не null, то при instantitateView будет установлен указанный цвет.
     */
    private @Nullable Integer tmpColor = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            ReadLaterItemParcelable itemParcelable = args.getParcelable(BUNDLE_ITEM_KEY);
            mCurrentItem = itemParcelable == null ? null : itemParcelable.getItem();
            mCurrentItemLocalId = args.getInt(BUNDLE_ITEMID_KEY, UID_EMPTY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstance) {

        View rootView = inflater.inflate(R.layout.fragment_edititem_viewpager, container, false);

        // Инициализация ViewPager и PagerAdapter
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager_edititem);
        EditItemPagerAdapter pagerAdapter = new EditItemPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(pagerAdapter);

        return rootView;

    }


    /////////////////////////
    // Колбеки EditItemFragmentActions

    /** {@inheritDoc}
     * Вызывается для текущего открытого фрагмента.
     */
    @Override
    public void onBackPressed() {
        if (mCurrentFragment != null) {
            mCurrentFragment.onBackPressed();
        }
    }

    /** {@inheritDoc}
     * Вызывается для текущего открытого фрагмента.
     */
    @Override
    public void setColor(int newColor) {
        if (mCurrentFragment != null) {
            mCurrentFragment.setColor(newColor);
        }
    }

}
