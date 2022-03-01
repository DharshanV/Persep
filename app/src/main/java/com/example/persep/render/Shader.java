package com.example.persep.render;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public class Shader
{
    private static final String TAG = "Shader";

    private int m_ProgramId;

    public void CreateShaderProgram(Context context, String vertexShaderPath, String fragmentShaderPath)
    {
        String vertexShaderCode = LoadStringFromAssetFile(context, vertexShaderPath);
        String fragmentShaderCode = LoadStringFromAssetFile(context, fragmentShaderPath);

        int vertexShader = LoadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = LoadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        m_ProgramId = GLES30.glCreateProgram();
        GLES30.glAttachShader(m_ProgramId, vertexShader);
        GLES30.glAttachShader(m_ProgramId, fragmentShader);
        GLES30.glLinkProgram(m_ProgramId);
    }

    public void UseProgram()
    {
        GLES30.glUseProgram(m_ProgramId);
    }

    public void SetMatrix4(String attribName, float[] matrix)
    {
        GLES30.glUniformMatrix4fv(GetUniformLocation(attribName), 1, false, matrix, 0);
    }

    public void SetFloatVertexArray(String attrib, int size, int stride, FloatBuffer buffer)
    {
        int arrayIndex = GetAttribLocation(attrib);
        GLES30.glEnableVertexAttribArray(arrayIndex);
        GLES30.glVertexAttribPointer(arrayIndex, size, GLES30.GL_FLOAT, false, stride, buffer);
    }

    public void SetInt(String attribName, int value)
    {
        GLES30.glUniform1i(GetAttribLocation(attribName), value);
    }

    public int GetAttribLocation(String attrib)
    {
        return GLES30.glGetAttribLocation(m_ProgramId, attrib);
    }

    public int GetUniformLocation(String uniform)
    {
        return GLES30.glGetUniformLocation(m_ProgramId, uniform);
    }

    private int LoadShader(int shaderType, String shaderSource)
    {
        int shader;
        int[] compiled = new int[1];

        shader = GLES30.glCreateShader(shaderType);

        if (shader == 0) return 0;

        GLES30.glShaderSource(shader, shaderSource);
        GLES30.glCompileShader(shader);
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0)
        {
            Log.e(TAG, GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
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

    public static Shader CreateShader(Context context, String vertPath, String fragPath)
    {
        Shader shader = new Shader();
        shader.CreateShaderProgram(context, vertPath, fragPath);
        return shader;
    }
}
