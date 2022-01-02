package com.example.persep.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.persep.render.RenderView;

//https://github.com/google/grafika/blob/b1df331e89cffeab621f02b102d4c2c25eb6088a/app/src/main/java/com/android/grafika/ContinuousCaptureActivity.java#L482
//https://www.maninara.com/2015/03/render-camera-preview-using-opengl-es.html
//https://www.freecodecamp.org/news/android-camera2-api-take-photos-and-videos/

public class VR_Fragment extends Fragment
{

    private static final String TAG = "VR_Fragment";

    RenderView m_RenderView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Created");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "Started");
        super.onStart();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Paused");
        super.onPause();
        m_RenderView.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resumed");
        super.onResume();
        m_RenderView.onResume();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        m_RenderView = new RenderView(getActivity());
        return m_RenderView;
    }
}