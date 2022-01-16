package com.example.persep.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.example.persep.data_gatherer.gyroscope.Gyroscope;

import java.util.concurrent.atomic.AtomicBoolean;

public class RenderView extends TextureView implements TextureView.SurfaceTextureListener, Gyroscope.Listener
{
    private static final String TAG = "RenderView";

    private GLProducerThread m_GLThread = null;
    private OpenGLRenderer m_Renderer;

    public RenderView(Context context)
    {
        super(context);
        m_Renderer = new OpenGLRenderer(context);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height)
    {
        m_GLThread = new GLProducerThread(surface, m_Renderer, new AtomicBoolean(true));
        m_GLThread.SetShouldRender(true);
        m_GLThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
    {
        m_Renderer.Resize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface)
    {
        Log.d(TAG, "Texture view destroyed");

        //Clean up running threads
        m_GLThread.SetShouldRender(false);
        m_GLThread.SetRunningFlag(false);
        try {  m_GLThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        m_GLThread = null;

        System.gc();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface)
    {
    }

    public void onPause()
    {
        if(m_GLThread != null)
        {
            m_GLThread.SetShouldRender(false);
        }
    }

    public void onResume()
    {
        if(m_GLThread != null)
        {
            m_GLThread.SetShouldRender(true);
        }
    }

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    @Override
    public void onRotation(float rx, float ry, float rz, int accuracy)
    {
    }
}
