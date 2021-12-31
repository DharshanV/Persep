package com.example.persep.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import com.example.persep.R;
import com.example.persep.adapters.SlidePagerAdapter;

public class HomeActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    SlidePagerAdapter slidePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        viewPager = findViewById(R.id.ViewPager);

        FragmentManager fragmentManager = getSupportFragmentManager();
        slidePagerAdapter = new SlidePagerAdapter(fragmentManager, getLifecycle());
        viewPager.setAdapter(slidePagerAdapter);
    }
}