package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.fragments.BasicFragmentCallbacks;
import com.example.mborzenkov.readlaterlist.fragments.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;

/** Фрагмент со списком ReadLaterItem.
 * Activity, использующая фрагмент, должна реализовывать интерфейс ItemListCallbacks.
 */
public class ItemListFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        ItemListAdapter.ItemListAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor>,
        FilterDrawerFragment.DrawerCallbacks {


    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_itemlist";


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект ItemListFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объектво FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @return новый объект ItemListFragment
     */
    public static ItemListFragment getInstance(FragmentManager fragmentManager) {

        ItemListFragment fragment = (ItemListFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new ItemListFragment();
        }

        return fragment;

    }

    /** Интерфейс для оповещений о событиях во фрагменте. */
    public interface ItemListCallbacks extends BasicFragmentCallbacks {

        /** Вызывается при нажатии на (+). */
        void onNewItemClick();

        /** Вызывается при клике на элемент списка.
         *
         * @param item элемент списка в формате ReadLaterItem
         * @param localId _id этого элемента
         */
        void onItemClick(@NonNull ReadLaterItem item, int localId);

        /** Вызывается при потягивании SwipeRefreshLayout или нажатии на кнопку Refresh. */
        void onRefreshToggled();

        /** Вызывается, когда список список обновился.
         * А именно, когда LoaderManager вернул onLoadFinished.
         *
         * @param isEmpty признак, пустой ли сейчас список
         */
        void onItemListReloaded(boolean isEmpty);
    }


    /////////////////////////
    // Поля объекта

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable ItemListCallbacks mCallbacks = null;

    // Хэлперы
    private @Nullable ItemListAdapter mItemListAdapter = null;
    private @Nullable ItemListLoaderManager mLoaderManager = null;
    private @Nullable FilterDrawerFragment mFilterDrawerFragment = null;

    // Объекты layout
    private DrawerLayout mDrawerLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mItemsListView;
    private LinearLayout mEmptyListView;


    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ItemListCallbacks) {
            mCallbacks = (ItemListCallbacks) context;
        }
        mItemListAdapter = new ItemListAdapter(context, this);
        mLoaderManager = new ItemListLoaderManager(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        // Инициализируем элементы layout
        mDrawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawerlayout_itemlist);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefreshlayout_itemlist);
        mItemsListView = (ListView) rootView.findViewById(R.id.listview_itemlist);
        mItemsListView.setAdapter(mItemListAdapter);
        mEmptyListView = (LinearLayout) rootView.findViewById(R.id.linearLayout_emptylist);

        // Инициализация Drawer Layout и обработчика открытия и закрытия Drawer
        mFilterDrawerFragment = (FilterDrawerFragment) getFragmentManager().findFragmentByTag(FilterDrawerFragment.TAG);
        if (mFilterDrawerFragment != null) {

            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout,
                    R.string.mainlist_drawer_title, R.string.mainlist_drawer_title) {

                @Override
                public void onDrawerClosed(View view) {
                    // При закрытии - устанавливаем фильтр
                    super.onDrawerClosed(view);
                    Log.d("ITEMLIST", "Drawer closed");
                    reloadData();
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    //  При открытии - обновляем Drawer на основании фильтра
                    super.onDrawerOpened(drawerView);
                    mFilterDrawerFragment.reloadDataFromCurrentFilter();
                }

            };
            mDrawerLayout.addDrawerListener(drawerToggle);
        }

        // Объекты и действия, имеющие смысл только при наличии колбеков
        if (mCallbacks != null) {
            // Инициализация Toolbar
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_itemlist);
            toolbar.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.icons));
            mCallbacks.setNewToolbar(toolbar);

            // Инициализируем FloatingActionButton
            FloatingActionButton floatingAddButton = (FloatingActionButton) rootView.findViewById(R.id.fab_item_add);
            floatingAddButton.setOnClickListener(view -> mCallbacks.onNewItemClick());

            // Слушаем о потягивании refresh
            mSwipeRefreshLayout.setOnRefreshListener(mCallbacks::onRefreshToggled);

            // Это нужно для того, чтобы swiperefresh не появлялся при скролле вверх
            mItemsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) { }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    int topRowVerticalPosition = (mItemsListView.getChildCount() == 0)
                            ?  0 : mItemsListView.getChildAt(0).getTop();
                    mSwipeRefreshLayout.setEnabled((firstVisibleItem == 0) && (topRowVerticalPosition >= 0));
                }
            });
        }

        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mItemListAdapter = null;
        mLoaderManager = null;
    }


    /////////////////////////
    // Колбеки Menu

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_mainlist, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Создание меню поиска
            SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.mainlist_action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setSubmitButtonEnabled(true);
            searchView.setOnQueryTextListener(this);
        }

        // TODO: Check it && swipe refresh layout
        if (mCallbacks == null) {
            menu.findItem(R.id.mainlist_action_refresh).setVisible(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_settings:
                mDrawerLayout.openDrawer(Gravity.END);
                return true;
            case R.id.mainlist_action_refresh:
                if (mCallbacks != null) {
                    mCallbacks.onRefreshToggled();
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /////////////////////////
    // Колбеки ввода текста (search)

    @Override
    public boolean onQueryTextSubmit(@NonNull String query) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String newText) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(newText);
        }
        return false;
    }


    /////////////////////////
    // Колбеки View.onClickListener

    @Override
    public void onClick(int position) {
        if ((mCallbacks != null) && (mItemListAdapter != null)) {
            // При нажатии на элемент, подготавливает и отправляет колбек.
            Cursor cursor = mItemListAdapter.getCursor();
            if (cursor != null && !cursor.isClosed()) {
                cursor.moveToPosition(position);
                int itemLocalId = cursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID);
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                ReadLaterItem item = dbAdapter.itemFromCursor(cursor);
                mCallbacks.onItemClick(item, itemLocalId);
            }
        }
    }


    /////////////////////////
    // Колбеки из ItemListLoaderManager

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        if (mLoaderManager != null) {
            switch (loaderId) {
                case ItemListLoaderManager.ITEM_LOADER_ID:
                    // Создаем новый CursorLoader, нужно все имеющееся в базе данных
                    return mLoaderManager.getNewCursorLoader();
                default:
                    throw new IllegalArgumentException("Loader Not Implemented: " + loaderId);
            }
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, @Nullable Cursor data) {
        // По завершению загрузки, подменяем Cursor в адаптере и показываем данные
        boolean listIsEmpty = true;
        if (mItemListAdapter != null) {
            mItemListAdapter.changeCursor(data);
            listIsEmpty = ((data == null) || (data.getCount() == 0));
            if (listIsEmpty) {
                mItemsListView.setVisibility(View.INVISIBLE);
                mEmptyListView.setVisibility(View.VISIBLE);
            } else {
                mEmptyListView.setVisibility(View.INVISIBLE);
                mItemsListView.setVisibility(View.VISIBLE);
            }
        }
        if (mCallbacks != null) {
            mCallbacks.onItemListReloaded(listIsEmpty);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        if (mItemListAdapter != null) {
            mItemListAdapter.changeCursor(null);
        }
    }


    /////////////////////////
    // Колбеки FilterDrawerFragment

    @Override
    public void onActionToggled(FilterDrawerFragment.DrawerActions action) {

        // Если выполняется какая-то работа, кнопки не работают, показывается предупреждение.
        if (mCallbacks != null && mCallbacks.isLongTaskActive()) {
            ActivityUtils.showAlertDialog(getContext(),
                    getString(R.string.mainlist_longloading_title),
                    getString(R.string.mainlist_longloading_text),
                    null,
                    null);
            return;
        }

        // TODO: Переделать бэкап таски
//
//        switch (action) {
//            case BACKUP_SAVE:
//                // Действие "Сохранить бэкап" открывает окно подтверждения и по положительному ответу
//                // вызывает функцию для сохранения
//                ActivityUtils.showAlertDialog(getContext(),
//                        getString(R.string.mainlist_drawer_backup_save_question_title),
//                        getString(R.string.mainlist_drawer_backup_save_question_text),
//                        () -> {
//                            handleBackupTask(true);
//                        },
//                        null);
//                break;
//            case BACKUP_RESTORE:
//                // Действие "Восстановить из бэкапа" открывает окно подтверждения и по положительному ответу
//                // вызывает функцию для восстановления
//                ActivityUtils.showAlertDialog(getContext(),
//                        getString(R.string.mainlist_drawer_backup_restore_question_title),
//                        getString(R.string.mainlist_drawer_backup_restore_question_text),
//                        () -> {
//                            handleBackupTask(false);
//                        },
//                        null);
//                break;
//            case FILL_PLACEHOLDERS:
//                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу
//                // вызывает функцию для заполнения
//                if (BuildConfig.DEBUG) {
//                    EditText inputNumber = new EditText(getContext());
//                    inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
//                    inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)}); // Не более 9
//                    ActivityUtils.showInputTextDialog(
//                            getContext(),
//                            inputNumber,
//                            getString(R.string.mainlist_menu_add_placeholders_question_title),
//                            getString(R.string.mainlist_menu_add_placeholders_question_text),
//                            (input) -> {
//                                try {
//                                    if (mCallbacks != null) {
//                                        // Смотрим введенное значение
//                                        int number = Integer.parseInt(input);
//                                        MainActivityLongTask.startLongBackgroundTask(
//                                                () -> DebugUtils.addPlaceholdersToDatabase(this, count),
//                                                this)){
//                                            showLoading();
//                                        }
//                                    }
//                                } catch (NumberFormatException e) {
//                                    Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
//                                }
//                            },
//                            null);
//                }
//                break;
//            case DELETE_ALL:
//                // Действие "Удалить все" открывает окно подтверждения и по положительному ответу
//                // вызывает функцию для очистки
//                if (BuildConfig.DEBUG) {
//                    ActivityUtils.showAlertDialog(
//                            getContext(),
//                            getString(R.string.mainlist_menu_delete_all_question_title),
//                            getString(R.string.mainlist_menu_delete_all_question_text),
//                            () -> {
//                                MainActivityLongTask.startLongBackgroundTask(
//                                        () -> {
//                                            ReadLaterDbUtils.deleteAll(this);
//                                            LongTaskNotifications.cancelNotification();
//                                        },
//                                        this)){
//                                    showLoading();
//                                }
//                            },
//                            null);
//                }
//                break;
//        }

        mDrawerLayout.closeDrawer(Gravity.END);
    }

    @Override
    public void onUserChanged() {
        // TODO: toggleSync();
    }


//    /////////////////////////
//    // Методы сохранения и восстановления из бэкапа
//    /** Выполняет сохранение или восстановление бэкапов в фоновом потоке.
//     *
//     * @param savingMode true - режим сохранения данных, false - режим восстановления
//     */
//    private void handleBackupTask(boolean savingMode) {
//
//        // Пробуем заблокировать интерфейс
//        if (!MainActivityLongTask.startAnotherLongTask()) {
//            return; // не удалось, что то уже происходит
//        }
//
//        // Запускаем поток
//        HandlerThread handlerThread = new HandlerThread("BackupHandlerThread");
//        handlerThread.start();
//        Looper looper = handlerThread.getLooper();
//        Handler handler = new Handler(looper);
//
//        // Выполняем работу
//        if (savingMode) {
//            handler.post(() -> {
//                MainListBackupUtils.saveEverythingAsJsonFile(this);
//                MainActivityLongTask.stopAnotherLongTask();
//            });
//        } else {
//            showLoading();
//            handler.post(() -> {
//                MainListBackupUtils.restoreEverythingFromJsonFile(this);
//                if (MainActivityLongTask.stopAnotherLongTask()) {
//                    runOnUiThread(mItemListFragment::reloadData);
//                }
//            });
//        }
//
//    }

    /////////////////////////
    // Все остальное

    /** Перезагружает данные в списке. */
    public void reloadData() {
        if (mLoaderManager != null) {
            mLoaderManager.reloadData();
        }
    }

    /** Проверяет, загружены ли данные в список.
     * Получает Cursor у адаптера и проверяет, что он не null.
     * Если данные загружены, но список пуст, то результат будет true.
     *
     * @return true, если данные загружены, иначе false
     */
    public boolean dataIsLoaded() {
        return (mItemListAdapter != null) && (mItemListAdapter.getCursor() != null);
    }


    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

}
