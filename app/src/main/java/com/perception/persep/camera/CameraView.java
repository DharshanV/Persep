package com.perception.persep.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;

import androidx.annotation.NonNull;

//https://github.com/yushulx/android-camera-preview/tree/master/camera2/app/src/main/java/com/dynamsoft/camera2
public class CameraView extends TextureView implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "PreviewCamera";

    private HandlerThread m_BackgroundThread;
    private Handler m_BackgroundHandler;
    private final Context m_Context;
    private PreviewCamera m_PreviewCamera;
    private Size m_TextureSize;

    private int m_WidthRatio = 0;
    private int m_HeightRatio = 0;

    public CameraView(Context context)
    {
        super(context);
        m_Context = context;
        setSurfaceTextureListener(this);
    }

    public void onPause()
    {
        if (m_PreviewCamera != null)
        {
            m_PreviewCamera.CloseCamera();
        }
    }

    public void onResume()
    {
        if (isAvailable() && m_PreviewCamera != null)
        {
            int width = m_TextureSize.getWidth();
            int height = m_TextureSize.getHeight();
            m_PreviewCamera.SetupCamera(width, height);
            Size previewSize = m_PreviewCamera.GetPreviewSize();
            m_PreviewCamera.OpenCamera(m_BackgroundHandler);
            setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        }
    }

    public void onStart()
    {
        StartBackgroundThread();
    }

    public void onStop()
    {
        StopBackgroundThread();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height)
    {
        StartBackgroundThread();
        m_TextureSize = new Size(width, height);
        m_PreviewCamera = new PreviewCamera(m_Context, surface);
        m_PreviewCamera.SetupCamera(width, height);
        Size previewSize = m_PreviewCamera.GetPreviewSize();
        m_PreviewCamera.OpenCamera(m_BackgroundHandler);
        setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
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

    public void TakeAndSavePreview(PreviewCamera.TakePictureListener listener)
    {
        m_PreviewCamera.TakeAndSavePreview(listener);
    }

    private void StartBackgroundThread()
    {
        if (m_BackgroundThread == null && m_BackgroundThread == null)
        {
            m_BackgroundThread = new HandlerThread("CameraBackground");
            m_BackgroundThread.start();
            m_BackgroundHandler = new Handler(m_BackgroundThread.getLooper());
        }
    }

    private void StopBackgroundThread()
    {
        try
        {
            if (m_BackgroundThread != null)
            {
                m_BackgroundThread.quitSafely();
                m_BackgroundThread.join();
            }
            m_BackgroundThread = null;
            m_BackgroundHandler = null;
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isValid()
    {
        return m_BackgroundThread != null && m_BackgroundHandler != null && m_PreviewCamera != null && m_PreviewCamera.isValid();
    }

    public void setAspectRatio(int width, int height)
    {
        if (width < 0 || height < 0)
        {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        m_WidthRatio = width;
        m_HeightRatio = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        if (0 == m_WidthRatio || 0 == m_HeightRatio)
//        {
//            setMeasuredDimension(width, height);
//        } else
//        {
//            if (width < height * m_WidthRatio / m_HeightRatio)
//            {
//                setMeasuredDimension(height * m_WidthRatio / m_HeightRatio, height);
//            } else
//            {
//                setMeasuredDimension(width, width * m_HeightRatio / m_WidthRatio);
//            }
//        }
    }

}