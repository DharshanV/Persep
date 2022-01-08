package com.example.persep.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

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

    private EGL10 mEgl;
    private EGLDisplay mEglDisplay = EGL10.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL10.EGL_NO_CONTEXT;
    private EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_OPENGL_ES2_BIT = 4;

    //Framerate
    private long m_StartTime;
    private long m_EndTime;
    private long m_DeltaTime;
    private static final int MAX_FPS = 30;

    public GLProducerThread(SurfaceTexture surfaceTexture, OpenGLRenderer renderer, AtomicBoolean runningFlag)
    {
        m_SurfaceTexture = surfaceTexture;
        m_Renderer = renderer;
        m_RunningFlag = runningFlag;
        m_ShouldRender = new AtomicBoolean();
//        startTime = System.currentTimeMillis();
    }

    private void initGLContext()
    {
        Log.d(TAG, "Initialized GL context");
        mEgl = (EGL10)EGLContext.getEGL();

        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetdisplay failed : " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int []version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed : " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int []configAttribs = {
                EGL10.EGL_BUFFER_SIZE, 32,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE
        };

        int []numConfigs = new int[1];
        EGLConfig[]configs = new EGLConfig[1];
        if (!mEgl.eglChooseConfig(mEglDisplay, configAttribs, configs, 1, numConfigs)) {
            throw new RuntimeException("eglChooseConfig failed : " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int []contextAttribs = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, contextAttribs);
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, configs[0], m_SurfaceTexture, null);
        if (mEglSurface == EGL10.EGL_NO_SURFACE || mEglContext == EGL10.EGL_NO_CONTEXT) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                throw new RuntimeException("eglCreateWindowSurface returned  EGL_BAD_NATIVE_WINDOW. " );
            }
            throw new RuntimeException("eglCreateWindowSurface failed : " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed : " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
    }

    private void destroyGLContext()
    {
        Log.d(TAG, "Destroyed GL context");
        mEgl.eglDestroyContext(mEglDisplay, mEglContext);
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
        mEglContext = EGL10.EGL_NO_CONTEXT;
        mEglSurface = EGL10.EGL_NO_SURFACE;

        m_Renderer = null;
    }

    @Override
    public void run()
    {
        initGLContext();

        if (m_Renderer != null) {  m_Renderer.initGL();  }

        while (m_RunningFlag != null && m_RunningFlag.get()) {
            m_StartTime = SystemClock.uptimeMillis() % 1000;

            if(m_ShouldRender != null && m_ShouldRender.get())
            {
                if (m_Renderer != null)
                {
                    m_Renderer.drawFrame();
                }
                mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
            }

            m_EndTime = SystemClock.uptimeMillis() % 1000;
            m_DeltaTime = m_EndTime - m_StartTime;

            if (m_DeltaTime < MAX_FPS) {
                try {  sleep(MAX_FPS - m_DeltaTime);
                } catch (InterruptedException ignored) { }
            }
        }

        destroyGLContext();
    }

    public void SetShouldRender(boolean shouldRender)
    {
        if(m_ShouldRender != null)
        {
            m_ShouldRender.set(shouldRender);
        }
    }

    public void SetRunningFlag(boolean runningFlag)
    {
        if(m_RunningFlag != null)
        {
            m_RunningFlag.set(runningFlag);
        }
    }
}
