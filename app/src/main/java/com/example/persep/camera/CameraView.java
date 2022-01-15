package com.example.persep.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;

import androidx.annotation.NonNull;

public class CameraView extends TextureView implements TextureView.SurfaceTextureListener
{
    private HandlerThread mBackgroundThread;
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
        stopBackgroundThread();
    }

    public void onResume()
    {
        startBackgroundThread();
        if(isAvailable() && m_PreviewCamera != null)
        {
            m_PreviewCamera.OpenCamera();
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
        startBackgroundThread();
        m_PreviewCamera = new PreviewCamera(m_Context, surface, m_BackgroundHandler);
        m_PreviewCamera.SetupCamera(width, height);
        m_PreviewCamera.OpenCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
    {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface)
    {
        m_PreviewCamera.CloseCamera();
        stopBackgroundThread();
        m_PreviewCamera = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface)
    {
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        m_BackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            m_BackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}