package com.example.persep.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLUtils;
import android.os.Trace;
import android.util.Log;

import com.example.persep.activities.HomeActivity;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class GLProducerThread extends Thread
{
    private static final String TAG = "GL_Thread";

    private AtomicBoolean m_RunningFlag;
    private AtomicBoolean m_ShouldRender;
    private SurfaceTexture m_SurfaceTexture;
    private OpenGLRenderer m_Renderer;
    private int m_SurfaceWidth;
    private int m_SurfaceHeight;

    private EGL10 m_Egl;
    private EGLDisplay m_EglDisplay = EGL10.EGL_NO_DISPLAY;
    private EGLContext m_EglContext = EGL10.EGL_NO_CONTEXT;
    private EGLSurface m_EglSurface = EGL10.EGL_NO_SURFACE;

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_OPENGL_ES2_BIT = 4;

    //Framerate
    private long m_StartTime;
    private long m_EndTime;
    private long m_DeltaTime;
    private float m_TargetFPS;
    private long m_TargetDelay;

    public GLProducerThread(SurfaceTexture surfaceTexture, OpenGLRenderer renderer, int width, int height, AtomicBoolean runningFlag)
    {
        m_SurfaceTexture = surfaceTexture;
        m_Renderer = renderer;
        m_SurfaceWidth = width;
        m_SurfaceHeight = height;
        m_RunningFlag = runningFlag;
        m_ShouldRender = new AtomicBoolean();
        m_StartTime = System.currentTimeMillis();

        m_TargetFPS = HomeActivity.DEFAULT_DISPLAY_REFRESH_RATE;
        m_TargetDelay = (long) (1.0f / m_TargetFPS * 1000.0f);
    }

    private void InitGLContext()
    {
        Log.d(TAG, "Initialized GL context");
        m_Egl = (EGL10) EGLContext.getEGL();

        m_EglDisplay = m_Egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (m_EglDisplay == EGL10.EGL_NO_DISPLAY)
        {
            throw new RuntimeException("eglGetdisplay failed : " + GLUtils.getEGLErrorString(m_Egl.eglGetError()));
        }

        int[] version = new int[2];
        if (!m_Egl.eglInitialize(m_EglDisplay, version))
        {
            throw new RuntimeException("eglInitialize failed : " + GLUtils.getEGLErrorString(m_Egl.eglGetError()));
        }

        int[] configAttribs =
        {
            EGL10.EGL_BUFFER_SIZE, 32,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_NONE
        };

        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        if (!m_Egl.eglChooseConfig(m_EglDisplay, configAttribs, configs, 1, numConfigs))
        {
            throw new RuntimeException("eglChooseConfig failed : " + GLUtils.getEGLErrorString(m_Egl.eglGetError()));
        }

        int[] contextAttribs = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE };
        m_EglContext = m_Egl.eglCreateContext(m_EglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, contextAttribs);
        m_EglSurface = m_Egl.eglCreateWindowSurface(m_EglDisplay, configs[0], m_SurfaceTexture, null);
        if (m_EglSurface == EGL10.EGL_NO_SURFACE || m_EglContext == EGL10.EGL_NO_CONTEXT)
        {
            int error = m_Egl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW)
            {
                throw new RuntimeException("eglCreateWindowSurface returned  EGL_BAD_NATIVE_WINDOW. ");
            }
            throw new RuntimeException("eglCreateWindowSurface failed : " + GLUtils.getEGLErrorString(m_Egl.eglGetError()));
        }

        if (!m_Egl.eglMakeCurrent(m_EglDisplay, m_EglSurface, m_EglSurface, m_EglContext))
        {
            throw new RuntimeException("eglMakeCurrent failed : " + GLUtils.getEGLErrorString(m_Egl.eglGetError()));
        }
    }

    private void DestroyGLContext()
    {
        Log.d(TAG, "Destroyed GL context");
        m_Egl.eglDestroyContext(m_EglDisplay, m_EglContext);
        m_Egl.eglDestroySurface(m_EglDisplay, m_EglSurface);
        m_EglContext = EGL10.EGL_NO_CONTEXT;
        m_EglSurface = EGL10.EGL_NO_SURFACE;

        m_Renderer = null;
    }

    @Override
    public void run()
    {
        Trace.beginSection("GL_Thread_Running");
        InitGLContext();

        if (m_Renderer != null)
        {
            m_Renderer.Initialize();
            m_Renderer.Resize(m_SurfaceWidth, m_SurfaceHeight);
        }

        while (m_RunningFlag != null && m_RunningFlag.get())
        {
            if (m_ShouldRender != null && m_ShouldRender.get())
            {
                Trace.beginSection("GL_Thread_Render");
                if (m_Renderer != null)
                {
                    m_Renderer.DrawFrame(m_DeltaTime);
                    m_Egl.eglSwapBuffers(m_EglDisplay, m_EglSurface);
                }
                Trace.endSection();
            }
            try { Thread.sleep(m_TargetDelay); } catch (InterruptedException ignored) { }
        }

        DestroyGLContext();
        Trace.endSection();
    }

    public void SetShouldRender(boolean shouldRender)
    {
        if (m_ShouldRender != null)
        {
            m_ShouldRender.set(shouldRender);
        }
    }

    public void SetRunningFlag(boolean runningFlag)
    {
        if (m_RunningFlag != null)
        {
            m_RunningFlag.set(runningFlag);
        }
    }
}
