package com.example.mborzenkov.readlaterlist.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

public class EditItemViewPagerFragment extends Fragment {

    /////////////////////////
    // PagerAdapter

    /** Адаптер для управления фрагментами EditItemFragment. */
    private class MainActivityPagerAdapter extends FragmentStatePagerAdapter {

        private FragmentManager mFragmentManager;
        private @Nullable
        ReadLaterItem mItem;
        private @IntRange(from = 0) int mItemLocalId;
        private @Nullable
        ImageView mSharedElement;

        private MainActivityPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            mFragmentManager = fragmentManager;
        }

        @Override
        public Fragment getItem(int position) {

            EditItemFragment editItemFragment = EditItemFragment.getInstance(mFragmentManager, (new ReadLaterItem.Builder("LABEL")).color(Color.RED).build(), 1);

            return editItemFragment;

        }

        @Override
        public int getCount() {
            return 1;
        }

    }


    private MainActivityPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edititem_viewpager, container, false);

        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager_edititem);


        // Инициализация PagerAdapter
        mPagerAdapter = new MainActivityPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityCompat.postponeEnterTransition(getActivity());
    }
}
