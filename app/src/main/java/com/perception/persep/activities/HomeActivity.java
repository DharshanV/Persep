package com.perception.persep.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.perception.persep.adapters.SlidePagerAdapter;
import com.perception.persep.data_gatherer.UserManager;
import com.perception.persep.fragments.UserFeed_Fragment;
import com.perception.persep.fragments.VR_Fragment;

import java.lang.reflect.Field;

public class HomeActivity extends AppCompatActivity
{
    private static final String TAG = "HomeActivity";

    public static HomeActivity m_HomeActivityInstance;
    public static float DEFAULT_DISPLAY_REFRESH_RATE = 0.0f;
    ViewPager2 m_ViewPager;
    SlidePagerAdapter m_SlidePagerAdapter;
    BottomNavigationView m_BottomNavigationView;

    public static Context getHomeActivityContext()
    {
        return m_HomeActivityInstance.getApplicationContext();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_HomeActivityInstance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        UserManager.getInstance().init();

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DEFAULT_DISPLAY_REFRESH_RATE = display.getRefreshRate();

        m_ViewPager = findViewById(R.id.ViewPager);
        m_ViewPager.setOffscreenPageLimit(SlidePagerAdapter.NUM_PAGES);

        FragmentManager fragmentManager = getSupportFragmentManager();
        m_SlidePagerAdapter = new SlidePagerAdapter(m_ViewPager, fragmentManager, getLifecycle());
        m_ViewPager.setAdapter(m_SlidePagerAdapter);
        m_ViewPager.setCurrentItem(SlidePagerAdapter.VR_FRAGMENT_INDEX, false);

        m_BottomNavigationView = findViewById(R.id.buttom_navigation);
        m_BottomNavigationView.setSelectedItemId(R.id.vr_view_menu);
        m_BottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.vr_view_menu)
            {
                m_ViewPager.setCurrentItem(SlidePagerAdapter.VR_FRAGMENT_INDEX, true);
            } else if (item.getItemId() == R.id.user_feed_menu)
            {
                m_ViewPager.setCurrentItem(SlidePagerAdapter.USERFEED_FRAGMENT, true);
            }
            return true;
        });

        for (int i = 0; i < m_BottomNavigationView.getMenu().size(); i++)
        {
            m_BottomNavigationView.findViewById(m_BottomNavigationView.getMenu().getItem(i).getItemId()).setOnLongClickListener(view -> true);
        }

        m_ViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
        {
            @Override
            public void onPageSelected(int position)
            {
                if (position == SlidePagerAdapter.VR_FRAGMENT_INDEX)
                {
                    m_BottomNavigationView.setSelectedItemId(R.id.vr_view_menu);
                } else if (position == SlidePagerAdapter.USERFEED_FRAGMENT)
                {
                    m_BottomNavigationView.setSelectedItemId(R.id.user_feed_menu);
                }
                super.onPageSelected(position);
            }
        });

        reduceDragSensitivity();
    }

    private void reduceDragSensitivity()
    {
        try
        {
            Field ff = ViewPager2.class.getDeclaredField("mRecyclerView");
            ff.setAccessible(true);
            RecyclerView recyclerView = (RecyclerView) ff.get(m_ViewPager);
            Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            Object touchSopObj = touchSlopField.get(recyclerView);
            if (touchSopObj != null)
            {
                int touchSlop = (int) touchSopObj;
                touchSlopField.set(recyclerView, touchSlop * 3);
            }
        } catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed()
    {
        int currentPageIndex = m_ViewPager.getCurrentItem();
        Fragment fragment = m_SlidePagerAdapter.m_PosFragmentMap.get(currentPageIndex);

        if (currentPageIndex == SlidePagerAdapter.VR_FRAGMENT_INDEX) {
            VR_Fragment vrFragment = (VR_Fragment) fragment;
            if (vrFragment != null && vrFragment.isPopupExpanded()) {
                vrFragment.collapsePopup();
            }
            else {
                super.onBackPressed();
            }
        }
        else if (currentPageIndex == SlidePagerAdapter.USERFEED_FRAGMENT){
            UserFeed_Fragment userFeedFragment = (UserFeed_Fragment) fragment;
            super.onBackPressed();
        }
    }
}