package com.perception.persep.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;

import androidx.annotation.NonNull;

public class CameraView extends TextureView implements TextureView.SurfaceTextureListener
{
    private HandlerThread m_BackgroundThread;
    private Handler m_BackgroundHandler;
    private Context m_Context;
    private PreviewCamera m_PreviewCamera;

    public CameraView(Context context)
    {
        super(context);
        m_Context = context;
        setSurfaceTextureListener(this);
    }

    public void onPause()
    {
        m_PreviewCamera.CloseCamera();
        StopBackgroundThread();
    }

    public void onResume()
    {
        StartBackgroundThread();
        if(isAvailable() && m_PreviewCamera != null)
        {
            m_PreviewCamera.OpenCamera(m_BackgroundHandler);
        }
    }

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height)
    {
        StartBackgroundThread();
        m_PreviewCamera = new PreviewCamera(m_Context, surface);
        m_PreviewCamera.SetupCamera(width, height);
        m_PreviewCamera.OpenCamera(m_BackgroundHandler);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
    {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface)
    {
        m_PreviewCamera.CloseCamera();
        StopBackgroundThread();
        m_PreviewCamera = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface)
    {
    }

    private void StartBackgroundThread()
    {
        m_BackgroundThread = new HandlerThread("CameraBackground");
        m_BackgroundThread.start();
        m_BackgroundHandler = new Handler(m_BackgroundThread.getLooper());
    }

    private void StopBackgroundThread()
    {
        try
        {
            m_BackgroundThread.quitSafely();
            m_BackgroundThread.join();
            m_BackgroundThread = null;
            m_BackgroundHandler = null;
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
}