package com.perception.persep.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.perception.persep.data_gatherer.GPSSensor;
import com.perception.persep.data_gatherer.RotationSensor;
import com.perception.persep.math_helpers.GeoLocationHelper;

import java.util.concurrent.atomic.AtomicBoolean;

public class RenderView extends TextureView implements TextureView.SurfaceTextureListener, RotationSensor.Listener, GPSSensor.Listener
{
    private static final String TAG = "RenderView";
    private final OpenGLRenderer m_Renderer;
    private GLProducerThread m_GLThread = null;

    public static final int GPS_MIN_DISTANCE = 3;                   //0 meters
    private Location m_PrevLocation = null;

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
    public void onLocationChanged(Location location)
    {
        if (m_PrevLocation == null) {
            m_Renderer.UpdateDeviceLocation(location.getLatitude(), location.getLongitude());
            m_PrevLocation = location;
            return;
        }

        double currDistance = GeoLocationHelper.Distance(location.getLatitude(), location.getLongitude(), m_PrevLocation.getLatitude(), m_PrevLocation.getLongitude());
        if (currDistance > GPS_MIN_DISTANCE) {
            m_Renderer.UpdateDeviceLocation(location.getLatitude(), location.getLongitude());
            m_PrevLocation = location;
        }
    }

    public String rayCast(float x, float y)
    {
        return m_Renderer.rayCast(x, y);
    }
}
