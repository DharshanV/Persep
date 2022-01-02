package com.example.persep.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;

public class RenderView extends GLSurfaceView
{
    private static final String TAG = "RenderView";

    OpenGLRenderer m_Renderer;

    public RenderView(Context context)
    {
        super(context);
        m_Renderer = new OpenGLRenderer();
        setEGLContextClientVersion(2);
        setRenderer(m_Renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.d(TAG, "OpenGL surface created");
        super.surfaceCreated(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.d(TAG, "OpenGL surface destroyed");
        super.surfaceDestroyed(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        Log.d(TAG, "OpenGL surface changed");
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resumed");
        super.onResume();
        m_Renderer.onResume();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Paused");
        super.onPause();
        m_Renderer.onPause();
    }
}
