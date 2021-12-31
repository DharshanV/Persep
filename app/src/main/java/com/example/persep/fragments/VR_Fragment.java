package com.example.persep.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.persep.R;

public class VR_Fragment extends Fragment {

    private static final String TAG = "VR_Fragment";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Created");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "Paused");
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG, "Started");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG, "Stopped");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "Resumed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.vr_fragment, container, false);
    }
}