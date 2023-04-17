package com.perception.persep.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.perception.persep.fragments.UserFeed_Fragment;
import com.perception.persep.fragments.VR_Fragment;

import java.util.HashMap;

public class SlidePagerAdapter extends FragmentStateAdapter
{
    public static final int USERFEED_FRAGMENT_1 = 0;
    public static final int VR_FRAGMENT_INDEX = 1;
    public static final int MIDDLE_PAGE_INDEX = 1;
    @ViewPager2.OffscreenPageLimit
    public static final int NUM_PAGES = 2;

    public HashMap<Integer, Fragment> m_PosFragmentMap = new HashMap<>();
    private ViewPager2 m_ViewPager;

    public SlidePagerAdapter(ViewPager2 viewPager, @NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle)
    {
        super(fragmentManager, lifecycle);
        m_ViewPager = viewPager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position)
    {
        Fragment fragment;
        switch (position)
        {
            case 0:
                fragment = new UserFeed_Fragment();
                break;
            case 1:
                fragment = new VR_Fragment(m_ViewPager);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + position);
        }
        m_PosFragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount()
    {
        return NUM_PAGES;
    }
}