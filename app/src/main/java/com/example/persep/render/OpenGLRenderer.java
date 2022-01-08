package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OpenGLRenderer
{
    private static final String TAG = "Renderer";

    private int m_ProgramObject;
    private FloatBuffer m_Vertices;

    private Context m_Context;
    private int m_Width;
    private int m_Height;

    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = {
            -0.5f, -0.5f, 0.0f, // left
            0.5f, -0.5f, 0.0f, // right
            0.0f,  0.5f, 0.0f  // top
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    OpenGLRenderer(Context context)
    {
        m_Vertices = ByteBuffer.allocateDirect(triangleCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_Vertices.put(triangleCoords).position(0);

        m_Context = context;
    }

    public void initGL()
    {
        Log.d(TAG, "Initialized renderer");

        comipleAndLinkProgram();

        GLES20.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
    }

    public void drawFrame()
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(m_ProgramObject);
        int mPositionHandle = GLES20.glGetAttribLocation(m_ProgramObject, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, m_Vertices);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public void resize(int width, int height)
    {
        Log.d(TAG, "Resized | width: " + width + " height: " + height);
        m_Width = width;
        m_Height = height;
        GLES20.glViewport(0, 0, m_Width, m_Height);
    }

    private int loadShader(int shaderType, String shaderSource) {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES20.glCreateShader(shaderType);

        if (shader == 0)
            return 0;

        // Load the shader source
        GLES20.glShaderSource(shader, shaderSource);

        // Compile the shader
        GLES20.glCompileShader(shader);

        // Check the compile status
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void comipleAndLinkProgram() {
        String vertexShaderCode =
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}";

        final String fragmentShaderCode =
                "precision mediump float;" +
                "void main() {" +
                "  gl_FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);" +
                "}";

        int vertexShader;
        int fragmentShader;
        int programObject;

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Create the program object
        programObject = GLES20.glCreateProgram();
        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);
        GLES20.glLinkProgram(programObject);
        m_ProgramObject = programObject;
    }
}
