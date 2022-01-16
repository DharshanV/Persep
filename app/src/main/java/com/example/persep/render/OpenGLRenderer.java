package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Trace;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OpenGLRenderer
{
    private static final String TAG = "Renderer";

    private int m_ProgramObject;
    private FloatBuffer m_VertexPosition;
    private int m_PositionIndex;
    private FloatBuffer m_VertexColor;
    private int m_ColorIndex;

    private Context m_Context;
    private int m_Width;
    private int m_Height;

    static float[] triangleCoords = {
            -0.5f, -0.5f, 0.0f, // left
            0.5f, -0.5f, 0.0f, // right
            0.0f,  0.5f, 0.0f  // top
    };
    static float[] triangleColor = {
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f,  0.0f, 1.0f
    };

    private float dimmingFactorVelocity = 0.01f;
    private float dimmingFactor = 0.0f;

    OpenGLRenderer(Context context)
    {
        m_VertexPosition = ByteBuffer.allocateDirect(triangleCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexPosition.put(triangleCoords).position(0);

        m_VertexColor = ByteBuffer.allocateDirect(triangleColor.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        m_VertexColor.put(triangleColor).position(0);

        m_Context = context;
    }

    public void InitGL()
    {
        Log.d(TAG, "Initialized renderer");

        CompileAndLinkProgram();

        m_PositionIndex = GLES20.glGetAttribLocation(m_ProgramObject, "inPosition");
        GLES20.glVertexAttribPointer(m_PositionIndex, 3, GLES20.GL_FLOAT, false, 3 * 4, m_VertexPosition);
        m_ColorIndex = GLES20.glGetAttribLocation(m_ProgramObject, "inColor");
        GLES20.glVertexAttribPointer(m_ColorIndex, 3, GLES20.GL_FLOAT, false, 3 * 4, m_VertexColor);

        GLES20.glEnableVertexAttribArray(m_PositionIndex);
        GLES20.glEnableVertexAttribArray(m_ColorIndex);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc( GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA );
    }

    public void DrawFrame(long deltaTime)
    {
        Trace.beginSection("gl_draw_frame");

        if (dimmingFactor > 1 || dimmingFactor < 0) dimmingFactorVelocity *= -1;
        dimmingFactor += dimmingFactorVelocity;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(m_ProgramObject);
        int dimmingFactorIndex = GLES20.glGetUniformLocation(m_ProgramObject, "dimmingFactor");
        GLES20.glUniform1f(dimmingFactorIndex, dimmingFactor);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        Trace.endSection();
    }

    public void Resize(int width, int height)
    {
        Log.d(TAG, "Resized | width: " + width + " height: " + height);
        m_Width = width;
        m_Height = height;
        GLES20.glViewport(0, 0, m_Width, m_Height);
    }

    private int LoadShader(int shaderType, String shaderSource) {
        int shader;
        int[] compiled = new int[1];

        shader = GLES20.glCreateShader(shaderType);

        if (shader == 0) return 0;

        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0)
        {
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private String LoadStringFromAssetFile(String filePath){
        try
        {
            StringBuilder shaderSource = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(m_Context.getAssets().open(filePath)));
            String line;
            while((line = reader.readLine()) != null)
            {
                shaderSource.append(line).append("\n");
            }
            reader.close();
            return shaderSource.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Could not load shader file");
            return null;
        }
    }

    private void CompileAndLinkProgram() {
        String vertexShaderCode = LoadStringFromAssetFile("fadingColorTriangle.vert");
        String fragmentShaderCode = LoadStringFromAssetFile("fadingColorTriangle.frag");

        int vertexShader = LoadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = LoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        m_ProgramObject = GLES20.glCreateProgram();
        GLES20.glAttachShader(m_ProgramObject, vertexShader);
        GLES20.glAttachShader(m_ProgramObject, fragmentShader);
        GLES20.glLinkProgram(m_ProgramObject);
    }
}
