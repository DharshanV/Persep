package com.perception.persep.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.perception.persep.fragments.Home_Fragment_1;
import com.perception.persep.fragments.Home_Fragment_3;
import com.perception.persep.fragments.VR_Fragment;

public class SlidePagerAdapter extends FragmentStateAdapter
{
    public static final int MIDDLE_PAGE_INDEX = 1;
    public static final int NUM_PAGES = 3;

    public SlidePagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle)
    {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        switch (position)
        {
            case 0:
                return new Home_Fragment_1();
            case 1:
                return new VR_Fragment();
        }
        return new Home_Fragment_3();
    }

    @Override
    public int getItemCount()
    {
        return NUM_PAGES;
    }
}