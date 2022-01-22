package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shader
{
    private static final String TAG = "Shader";

    private String m_VertexShaderPath;
    private String m_FragmentShaderPath;
    private int m_ProgramId;

    public Shader(String vertexShaderPath, String fragmentShaderPath)
    {
        m_VertexShaderPath = vertexShaderPath;
        m_FragmentShaderPath = fragmentShaderPath;
    }

    public void CreateShaderProgram(Context context)
    {
        String vertexShaderCode = LoadStringFromAssetFile(context, m_VertexShaderPath);
        String fragmentShaderCode = LoadStringFromAssetFile(context, m_FragmentShaderPath);

        int vertexShader = LoadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = LoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        m_ProgramId = GLES20.glCreateProgram();
        GLES20.glAttachShader(m_ProgramId, vertexShader);
        GLES20.glAttachShader(m_ProgramId, fragmentShader);
        GLES20.glLinkProgram(m_ProgramId);
    }

    public void UseProgram()
    {
        GLES20.glUseProgram(m_ProgramId);
    }

    public int GetAttribLocation(String attrib)
    {
        return GLES20.glGetAttribLocation(m_ProgramId, attrib);
    }

    public int GetUniformLocation(String uniform)
    {
        return GLES20.glGetUniformLocation(m_ProgramId, uniform);
    }

    private int LoadShader(int shaderType, String shaderSource)
    {
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

    private String LoadStringFromAssetFile(Context context, String filePath)
    {
        try
        {
            StringBuilder shaderSource = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filePath)));
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
}
