package com.example.persep.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

//https://programmer.group/writing-opengl-program-based-on-textureview.html
public class RenderView extends TextureView implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "RenderView";
    private GLProducerThread m_ProducerThread = null;
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
        m_ProducerThread = new GLProducerThread(surface, m_Renderer, new AtomicBoolean(true));
        m_ProducerThread.SetShouldRender(true);
        m_ProducerThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
    {
        m_Renderer.resize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface)
    {
        Log.d(TAG, "Texture view destroyed");
        m_ProducerThread.SetShouldRender(false);
        m_ProducerThread.SetRunningFlag(false);
        try
        {
            m_ProducerThread.join();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_ProducerThread = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface)
    {
    }

    public void onPause()
    {
        if(m_ProducerThread != null)
        {
            m_ProducerThread.SetShouldRender(false);
        }
    }

    public void onResume()
    {
        if(m_ProducerThread != null)
        {
            m_ProducerThread.SetShouldRender(true);
        }
    }
}
