package com.example.persep.shapes;

import android.content.Context;
import android.opengl.GLES30;

import com.example.persep.render.Shader;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class AlbumModel
{
    static private final FloatBuffer[] m_VertexPosition = new FloatBuffer[2];
    static private final FloatBuffer[] m_VertexNormals = new FloatBuffer[2];
    static private final IntBuffer[] m_VertexIndices = new IntBuffer[2];
    static private final FloatBuffer[] m_VertexUV = new FloatBuffer[2];
    static private final int[] m_NumOfIndices = new int[2];

    private final Shader[] m_Shaders = new Shader[2];

    private static final String[] ALBUM_MODEL_PATH = { "album_frame.obj", "album_surface.obj" };
    private static final String[] ALBUM_FRAME_SHADER_PATH = { "album_frame.vert", "album_frame.frag" };
    private static final String[] ALBUM_SURFACE_SHADER_PATH = { "album_surface.vert", "album_surface.frag" };

    public void Draw()
    {
        for(int i = 0; i < 2; i++)
        {
            m_Shaders[i].UseProgram();
            m_Shaders[i].SetFloatVertexArray("iPosition", 3, 3 * 4, m_VertexPosition[i]);
            m_Shaders[i].SetFloatVertexArray("iUV", 2, 2 * 4, m_VertexUV[i]);
            m_Shaders[i].SetFloatVertexArray("iNormal", 3, 3 * 4, m_VertexNormals[i]);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, m_NumOfIndices[i] * 3, GLES30.GL_UNSIGNED_INT, m_VertexIndices[i]);
        }
    }

    static private void LoadModelBuffers(Context context, int i)
    {
        InputStream in = null;
        try
        {
            in = context.getAssets().open(ALBUM_MODEL_PATH[i]);
            Obj obj = ObjUtils.convertToRenderable(ObjReader.read(in));
            m_NumOfIndices[i] = obj.getNumFaces();
            m_VertexIndices[i] = ObjData.getFaceVertexIndices(obj, 3);
            m_VertexPosition[i] = ObjData.getVertices(obj);
            m_VertexUV[i] = ObjData.getTexCoords(obj, 2);
            m_VertexNormals[i] = ObjData.getNormals(obj);
        } catch (Exception ex) { throw new RuntimeException("couldn't load '" + ALBUM_MODEL_PATH[i] + "'", ex); }
        finally
        {
            if (in != null)
            {
                try { in.close(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }

    static public AlbumModel LoadModel(Context context)
    {
        Thread frameLoadThread = new Thread(() -> LoadModelBuffers(context, 0));
        frameLoadThread.start();

        Thread surfaceLoadThread = new Thread(() -> LoadModelBuffers(context, 1));
        surfaceLoadThread.start();

        try
        {
            frameLoadThread.join();
            surfaceLoadThread.join();
        }
        catch (Exception ignored) {}

        return new AlbumModel();
    }

    public void BuildShaders(Context context)
    {
        m_Shaders[0] = Shader.CreateShader(context, ALBUM_FRAME_SHADER_PATH[0], ALBUM_FRAME_SHADER_PATH[1]);
        m_Shaders[1] = Shader.CreateShader(context, ALBUM_SURFACE_SHADER_PATH[0], ALBUM_SURFACE_SHADER_PATH[1]);
    }

    public void SetMVP(float[] M, float[] V, float[] P)
    {
        for(int i = 0; i < 2; i++)
        {
            m_Shaders[i].UseProgram();
            m_Shaders[i].SetMatrix4("model", M);
            m_Shaders[i].SetMatrix4("view", V);
            m_Shaders[i].SetMatrix4("projection", P);
        }
    }
}
