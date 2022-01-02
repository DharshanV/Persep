package com.example.persep.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

public class RenderView extends GLSurfaceView
{
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
        super.surfaceCreated(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        super.surfaceDestroyed(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        m_Renderer.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        m_Renderer.onPause();
    }
}
