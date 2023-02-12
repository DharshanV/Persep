package com.perception.persep.shapes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.perception.persep.render.Shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class AlbumModel
{
    private static final String TAG = "AlbumModel";

    static private final int FRAME = 0;
    static private final int SURFACE = 1;
    static private final FloatBuffer[] m_VertexPosition = new FloatBuffer[2];
    static private final FloatBuffer[] m_VertexNormals = new FloatBuffer[2];
    static private final IntBuffer[] m_VertexIndices = new IntBuffer[2];
    static private final FloatBuffer[] m_VertexUV = new FloatBuffer[2];
    static private final int[] m_NumOfIndices = new int[2];

    private final Shader[] m_Shaders = new Shader[2];
    private int[] m_TextureID = null;

    private static final String[] ALBUM_MODEL_PATH = {"album_frame.obj", "album_surface.obj"};
    private static final String[] ALBUM_FRAME_SHADER_PATH = {"album_frame.vert", "album_frame.frag"};
    private static final String[] ALBUM_SURFACE_SHADER_PATH = {"album_surface.vert", "album_surface.frag"};

    public void DrawFrame()
    {
        m_Shaders[FRAME].UseProgram();
        m_Shaders[FRAME].SetFloatVertexArray("iPosition", 3, 3 * 4, m_VertexPosition[FRAME]);
        m_Shaders[FRAME].SetFloatVertexArray("iUV", 2, 2 * 4, m_VertexUV[FRAME]);
        m_Shaders[FRAME].SetFloatVertexArray("iNormal", 3, 3 * 4, m_VertexNormals[FRAME]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, m_NumOfIndices[FRAME] * 3, GLES30.GL_UNSIGNED_INT, m_VertexIndices[FRAME]);
    }

    public void DrawSurface(int textureID)
    {
        m_Shaders[SURFACE].UseProgram();
        m_Shaders[SURFACE].SetFloatVertexArray("iPosition", 3, 3 * 4, m_VertexPosition[SURFACE]);
        m_Shaders[SURFACE].SetFloatVertexArray("iUV", 2, 2 * 4, m_VertexUV[SURFACE]);
        m_Shaders[SURFACE].SetFloatVertexArray("iNormal", 3, 3 * 4, m_VertexNormals[SURFACE]);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureID);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, m_NumOfIndices[SURFACE] * 3, GLES30.GL_UNSIGNED_INT, m_VertexIndices[SURFACE]);
    }

    public void UpdateSurfaceTexture(int textureID, Bitmap image)
    {
        m_Shaders[SURFACE].UseProgram();
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureID);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, image, 0);
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
        m_Shaders[FRAME] = Shader.CreateShader(context, ALBUM_FRAME_SHADER_PATH[0], ALBUM_FRAME_SHADER_PATH[1]);
        m_Shaders[SURFACE] = Shader.CreateShader(context, ALBUM_SURFACE_SHADER_PATH[0], ALBUM_SURFACE_SHADER_PATH[1]);
    }

    public void LoadTextures(Context context, String[] imagePaths)
    {
        m_TextureID = new int[imagePaths.length];

        // load textures into album_surface shader
        m_Shaders[SURFACE].UseProgram();
        GLES30.glGenTextures(imagePaths.length, m_TextureID, 0);

        try
        {
            for (int i = 0; i < imagePaths.length; i++)
            {
                Bitmap bitMap = BitmapFactory.decodeStream(context.getAssets().open(imagePaths[i]));
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, m_TextureID[i]);

                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitMap, 0);

                bitMap.recycle();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Could not load image file");
        }
    }

    public Shader GetFrameShader()
    {
        return m_Shaders[FRAME];
    }

    public Shader GetSurfaceShader()
    {
        return m_Shaders[SURFACE];
    }

    public void SetMVP(float[] M, float[] V, float[] P)
    {
        for (int i = 0; i < 2; i++)
        {
            m_Shaders[i].UseProgram();
            m_Shaders[i].SetMatrix4("model", M);
            m_Shaders[i].SetMatrix4("view", V);
            m_Shaders[i].SetMatrix4("projection", P);
        }
    }
}
