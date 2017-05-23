package com.example.mborzenkov.readlaterlist.fragments.edititem;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;

/** Фрагмент для ViewPager с фрагментами EditItemFragment.
 * Использование:
 *      Для получения объекта всегда используйте getInstance.
 *      Для заполнения фрагмента данными, необходимо передать в getInstance объект ReadLaterItem, его itemLocalId,
 *          позицию объекта и общее число объектов в наборе данных.
 *      Activity, использующая фрагмент, должна реализовывать интерфейс EditItemViewPagerCallbacks, чтобы фрагмент
 *          мог получать данные о других объектах в наборе данных динамически.
 *      Фрагменты EditItemFragment во ViewPager для колбеков используют Activity, к которой они привязаны в onDetach.
 *      Для получения результатов редактирования, необходимо, чтобы Activity, использующая фрагмент, реализовывала
 *          интерфейс EditItemCallbacks.
 */
public class EditItemViewPagerFragment extends Fragment
        implements EditItemFragmentActions {

    // TODO: [ViewPager] Не работает меню в первом фрагменте (и кнопка <-)

    // TODO: [ViewPager] Табы

    // TODO: [ViewPager] Проверить без интернета на больших данных (20 шт.) туда сюда что все работает
    // TODO: [ViewPager] Убрать данные, добавить штук 5 и потом влкючить интернет при редактировании, должен пойти синк
    // TODO: [ViewPager] Комментарии по ViewPager и инспекторы


    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_edititem_viewpager";

    /** Ключ для Bundle с позицией элемента. */
    public static final String BUNDLE_ITEMPOSITION_KEY = "item_position";
    /** Ключ для Bundle с общим количеством элементов. */
    public static final String BUNDLE_TOTALITEMS_KEY = "total_items";


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
            EditItemFragment fragment = null;
            if (position == mCurrentItemPosition) {
                fragment = EditItemFragment.getInstance(mFragmentManager, mCurrentItem, mCurrentItemLocalId);
            } else if (mCallbacks != null) {
                fragment = EditItemFragment.getInstance(mFragmentManager,
                        mCallbacks.getItemAt(position),
                        mCallbacks.getItemLocalIdAt(position));
            }
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if ((position == mViewPager.getCurrentItem()) && (obj instanceof EditItemFragment) && (tmpColor != null)) {
                EditItemFragment fragment = (EditItemFragment) obj;
                fragment.setColor(tmpColor);
                tmpColor = null;
            }
            return obj;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (mCurrentFragment != object) {
                Log.d("FRAGMENT", "SET PRIM: " + object + " at " + position);
                if (mCurrentFragment != null) {
                    mCurrentFragment.setSharedElementTransitionName(null);
                }
                mCurrentFragment = (EditItemFragment) object;
                mCurrentItemPosition = position;
                if (mCurrentFragment != null) {
                    mCurrentFragment.setSharedElementTransitionName(MainActivity.SHARED_ELEMENT_COLOR_TRANSITION_NAME);
                }
                if (mCallbacks != null) {
                    mCurrentItem = mCallbacks.getItemAt(position);
                    mCurrentItemLocalId = mCallbacks.getItemLocalIdAt(position);
                }
            }
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public int getCount() {
            return mTotalItems;
        }

    }


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект EditItemViewPagerFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     * Параметры item и itemLocalId используются для установки открываемого фрагмента.
     * Параметры position и totalItems описывают границы и текущий объект в наборе данных.
     * Activity, использующая фрагмент, должна реализовывать интерфейс EditItemViewPagerCallbacks, чтобы фрагмент
     *          мог получать данные о других объектах в наборе данных динамически.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @param item объект для редактирования или null, если создание нового элемента
     * @param itemLocalId внутренний идентификатор объекта или UID_EMPTY, если создание нового элемента
     * @param position позиция элемента в наборе данных, >=0 и < totalItems
     * @param totalItems общее число элементов в наборе данных, если создание нового элемента, то 1, иначе >=1
     *
     * @return объект EditItemViewPagerFragment
     *
     * @throws IllegalArgumentException если itemLocalId < UID_EMPTY
     * @throws IllegalArgumentException если item == null и itemLocalId != -UID_EMPTY
     * @throws IllegalArgumentException если position < 0 или position >= totalItems
     * @throws IllegalArgumentException если totalItems < 1
     */
    public static EditItemViewPagerFragment getInstance(FragmentManager fragmentManager,
                                                        @Nullable ReadLaterItem item,
                                                        @IntRange(from = UID_EMPTY) int itemLocalId,
                                                        @IntRange(from = 0) int position,
                                                        @IntRange(from = 1) int totalItems) {

        if ((itemLocalId < UID_EMPTY) || ((item == null) && (itemLocalId != UID_EMPTY))) {
            throw new IllegalArgumentException(
                    String.format("Erorr @ EditItemViewPagerFragment.getInstance. itemLocalId: %s, item: %s",
                            itemLocalId, item));
        } else if ((position < 0) || (position >= totalItems)) {
            throw new IllegalArgumentException("Erorr @ EditItemViewPagerFragment.getInstance. position = " + position);
        } else if (totalItems < 1) {
            throw new IllegalArgumentException(
                    "Erorr @ EditItemViewPagerFragment.getInstance. totalItems = " + totalItems);
        }

        EditItemViewPagerFragment fragment = (EditItemViewPagerFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new EditItemViewPagerFragment();
        }

        Bundle args = new Bundle();
        if (item != null) {
            args.putParcelable(BUNDLE_ITEM_KEY, new ReadLaterItemParcelable(item));
            args.putInt(BUNDLE_ITEMID_KEY, itemLocalId);
            fragment.setArguments(args);
        } else {
            fragment.setArguments(null);
        }
        args.putInt(BUNDLE_ITEMPOSITION_KEY, position);
        args.putInt(BUNDLE_TOTALITEMS_KEY, totalItems);

        return fragment;

    }

    /** Интерфейс для связи между объектом, предоставляющим данные и этим ViewPager. */
    public interface EditItemViewPagerCallbacks {

        /** Возвращает элемент в наборе данных на указанной позиции.
         *
         * @param position позиция элемента в наборе данных (начиная с 0)
         *
         * @return объект ReadLaterItem, соответствующий позиции position
         *          или null, если элемента на такой позиции нет
         *
         * @throws IllegalArgumentException если position < 0 или position >= числа элементов в наборе данных
         */
        @Nullable ReadLaterItem getItemAt(@IntRange(from = 0) int position);

        /** Возвращает внутренний идентификатор элемента на указанной позиции.
         *
         * @param position позиция элемента в наборе данных (начиная с 0)
         *
         * @return внутреннией идентификатор элемента на позиции position
         *          или UID_EMPTY, если элемента на такой позиции нет
         *
         * @throws IllegalArgumentException если position < 0 или position >= числа элементов в наборе данных
         */
        @IntRange(from = UID_EMPTY) int getItemLocalIdAt(@IntRange(from = 0) int position);
    }


    /////////////////////////
    // Поля объекта

    // Инвариант
    //      mCurrentItem - текущий редактируемый элемент. Null, если создается новый объект, иначе не null
    //      mCurrentItemLocalId - внутренний идентификатор элемента. UID_EMPTY, если создается новый объект, иначе >= 0
    //      mCurrentItemPosition - позиция текущего редактируемого элемента. 0, если создается новый объект, иначе >= 0
    //      mTotalItems - общее число элементов. 1, если создается новый объект, иначе > 1

    /** Текущий редактируемый элемент. */
    private @Nullable ReadLaterItem mCurrentItem = null;
    /** Внутренний идентификатор текущего редактируемого элемента. */
    private @IntRange(from = UID_EMPTY) int mCurrentItemLocalId = UID_EMPTY;
    /** Позиция текущего редактируемого элемента. */
    private @IntRange(from = 0) int mCurrentItemPosition = 0;
    /** Общее количество элементов. */
    private @IntRange(from = 1) int mTotalItems = 1;

    // Хэлперы
    private @Nullable EditItemFragment mCurrentFragment = null; // Не null, если уже прошел instantiateItem у адаптера.
    private @Nullable EditItemViewPagerCallbacks mCallbacks = null;

    // Элементы layout
    private ViewPager mViewPager;

    /** Поле для сохранения цвета при переходе из ColorPicker в EditItemFragment.
     * Необходимо из-за промежутка между закрытием ColorPickerFragment и вызовом instantiateView в PagerAdapter.
     * Если не null, то при instantitateView будет установлен указанный цвет.
     */
    private @Nullable Integer tmpColor = null;


    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EditItemViewPagerCallbacks) {
            mCallbacks = (EditItemViewPagerCallbacks) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем данные из savedInstanceState, если есть, или из arguments
        if ((savedInstanceState != null)
                && savedInstanceState.containsKey(BUNDLE_ITEM_KEY)
                && savedInstanceState.containsKey(BUNDLE_ITEMID_KEY)
                && savedInstanceState.containsKey(BUNDLE_ITEMPOSITION_KEY)
                && savedInstanceState.containsKey(BUNDLE_TOTALITEMS_KEY)) {

            ReadLaterItemParcelable itemParcelable = savedInstanceState.getParcelable(BUNDLE_ITEM_KEY);
            mCurrentItem = itemParcelable == null ? null : itemParcelable.getItem();
            mCurrentItemLocalId = savedInstanceState.getInt(BUNDLE_ITEMID_KEY, UID_EMPTY);
            mCurrentItemPosition = savedInstanceState.getInt(BUNDLE_ITEMPOSITION_KEY, 0);
            mTotalItems = savedInstanceState.getInt(BUNDLE_TOTALITEMS_KEY, 0);

        } else {

            Bundle args = getArguments();
            if (args != null) {
                ReadLaterItemParcelable itemParcelable = args.getParcelable(BUNDLE_ITEM_KEY);
                mCurrentItem = itemParcelable == null ? null : itemParcelable.getItem();
                mCurrentItemLocalId = args.getInt(BUNDLE_ITEMID_KEY, UID_EMPTY);
                mCurrentItemPosition = args.getInt(BUNDLE_ITEMPOSITION_KEY, 0);
                mTotalItems = args.getInt(BUNDLE_TOTALITEMS_KEY, 0);
            }

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
        mViewPager.setCurrentItem(mCurrentItemPosition, false);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                // Проверим для фрагмента, не изменен ли он, если что не даем скролить
                if ((mCurrentFragment != null)
                        && (mCurrentItemPosition != position)
                        && (mCurrentFragment.isModified())) {

                    int lastPosition = mCurrentItemPosition;
                    EditItemFragment lastFragment = mCurrentFragment;
                    mCurrentFragment.showModifiedAlertWithOptions(
                            () -> lastFragment.reloadDataFromItem(null), // reload data
                            () -> mViewPager.setCurrentItem(lastPosition)
                    );

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        return rootView;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ReadLaterItemParcelable itemParc = mCurrentItem == null ? null : new ReadLaterItemParcelable(mCurrentItem);
        outState.putParcelable(BUNDLE_ITEM_KEY, itemParc);
        outState.putInt(BUNDLE_ITEMID_KEY, mCurrentItemLocalId);
        outState.putInt(BUNDLE_ITEMPOSITION_KEY, mCurrentItemPosition);
        outState.putInt(BUNDLE_TOTALITEMS_KEY, mTotalItems);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
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
        tmpColor = newColor;
    }

}
