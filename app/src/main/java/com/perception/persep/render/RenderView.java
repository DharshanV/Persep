package com.perception.persep.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.perception.persep.data_gatherer.GPSSensor;
import com.perception.persep.data_gatherer.RotationSensor;

import java.util.concurrent.atomic.AtomicBoolean;

public class RenderView extends TextureView implements TextureView.SurfaceTextureListener, RotationSensor.Listener, GPSSensor.Listener
{
    private static final String TAG = "RenderView";
    private final OpenGLRenderer m_Renderer;
    private GLProducerThread m_GLThread = null;

    public RenderView(Context context)
    {
        super(context);
        m_Renderer = new OpenGLRenderer(context);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height)
    {
        m_GLThread = new GLProducerThread(surface, m_Renderer, width, height, new AtomicBoolean(true));
        m_GLThread.SetShouldRender(true);
        m_GLThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
    {
        m_GLThread.Resize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface)
    {
        Log.d(TAG, "Texture view destroyed");

        //Clean up running threads
        m_GLThread.SetShouldRender(false);
        m_GLThread.SetRunningFlag(false);
        try
        {
            m_GLThread.join();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
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
        if (m_GLThread != null)
        {
            m_GLThread.SetShouldRender(false);
        }
    }

    public void onResume(boolean shouldRender)
    {
        if (m_GLThread != null)
        {
            m_GLThread.SetShouldRender(shouldRender);
        }
    }

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    public void ShouldRender(boolean value)
    {
        if (m_GLThread != null)
        {
            m_GLThread.SetShouldRender(value);
        }
    }

    @Override
    public void onRotationDelta(float[] deltaOrientation)
    {
        m_Renderer.UpdateRotation(deltaOrientation);
    }

    @Override
    public void onLocationChanged(double lat, double lon)
    {
        m_Renderer.UpdateDeviceLocation(lat, lon);
    }

//    @Override
//    public void onAlbumUpdate(List<Album> albums)
//    {
//        m_Renderer.onAlbumUpdate(albums);
//    }


    public String rayCast(float x, float y)
    {
        return m_Renderer.rayCast(x, y);
    }
}
