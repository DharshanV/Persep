package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.WriteBatch;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.data_gatherer.AlbumManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.enums.AlbumError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Album
{
    public final List<String> m_PostIDs = new ArrayList<>();
    private final Set<Integer> m_PendingPostUpdate = new HashSet<>();
    private final Timer m_UpdatePostTimer;
    private final int TASK_START_DELAY = 5000; // 5 secs
    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_WriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_ReadLock = m_ReadWriteLock.writeLock();
    public String m_AlbumID;
    public String m_GeoHash;
    public GeoPoint m_GeoPoint;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public DocumentReference m_DocRef;
    private int m_CurrPostIndex = 0;
    private boolean m_ShouldUpdateTexture = false;
    private PostUpdateIndexTask m_PostUpdateTimerTask;
    private Bitmap m_AlbumUI = null;
    private Paint m_Paint;
    private Canvas m_UICanvas;
    private int m_CanvasWidth;
    private int m_CanvasHeight;
    private Path m_HeartPath;

    public Album(double lat, double lon)
    {
        m_GeoPoint = new GeoPoint(lat, lon);
        m_GeoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lon));
        m_TimeCreated = Timestamp.now();
        m_TimeModified = m_TimeCreated;
        createAlbumServer();

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
        init();
    }

    public Album(DocumentSnapshot albumDoc)
    {
        m_AlbumID = albumDoc.getId();
        m_DocRef = albumDoc.getReference();

        try
        {
            m_GeoPoint = (GeoPoint) albumDoc.get(AlbumConstants.GEO_POINT);
            m_GeoHash = (String) albumDoc.get(AlbumConstants.GEO_HASH);
            m_TimeCreated = (Timestamp) albumDoc.get(AlbumConstants.TIME_CREATED);
            m_TimeModified = (Timestamp) albumDoc.get(AlbumConstants.TIME_MODIFIED);

            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
            for (DocumentReference postRef : postRefs)
            {
                PostManager.getInstance().addPost(postRef, (success, message, postID) -> {
                    if (success)
                    {
                        m_WriteLock.lock();
                        m_PostIDs.add(postID);
                        m_PendingPostUpdate.add(m_PostIDs.size() - 1);
                        updateUIOnPostChanged();
                        m_WriteLock.unlock();
                    } else
                    {
                        AlbumManager.getInstance().removeInvalidAlbumPost(m_AlbumID, postRef);
                    }
                });
            }
        } catch (NullPointerException e)
        {
            AlbumManager.getInstance().onAlbumError(m_AlbumID, null, AlbumError.CREATE_DOCUMENT_SNAPSHOT);
        }

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
        init();
    }

    static int mod(int a, int b)
    {
        int ret = a % b;
        if (ret < 0) ret += b;
        return ret;
    }

    private void init()
    {
        m_CanvasWidth = 1280;
        m_CanvasHeight = 720;
        m_AlbumUI = Bitmap.createBitmap(m_CanvasWidth, m_CanvasHeight, Bitmap.Config.ARGB_4444);

        m_Paint = new Paint();
        m_Paint.setAntiAlias(true);

        m_UICanvas = new Canvas(m_AlbumUI);
        m_UICanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float ratio = ((float) m_CanvasWidth / (float) m_CanvasHeight) - 1.0f;
        m_UICanvas.scale(1.0f, ratio);

        m_HeartPath = createHeartPath();
    }

    private void createAlbumServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document();
        m_AlbumID = m_DocRef.getId();

        Map<String, Object> albumData = new HashMap<>();
        albumData.put(AlbumConstants.GEO_HASH, m_GeoHash);
        albumData.put(AlbumConstants.GEO_POINT, m_GeoPoint);
        albumData.put(AlbumConstants.POSTS, Collections.emptyList());
        albumData.put(AlbumConstants.TIME_CREATED, m_TimeCreated);
        albumData.put(AlbumConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.set(albumData).addOnFailureListener(e -> AlbumManager.getInstance().onAlbumError(m_AlbumID, null, AlbumError.SET_DOCUMENT_REF));
    }

    public void remove()
    {
        m_ReadLock.lock();
        for (String postID : m_PostIDs)
        {
            Post post = PostManager.getInstance().getPost(postID);
            if (post != null)
            {
                post.remove();
            }
        }
        m_ReadLock.unlock();
        m_WriteLock.lock();
        m_PostIDs.clear();
        m_WriteLock.unlock();
        m_DocRef.delete();
    }

    public void unload()
    {
        m_ReadLock.lock();
        for (String postID : m_PostIDs)
        {
            PostManager.getInstance().unloadPost(postID);
        }
        m_ReadLock.unlock();

        m_WriteLock.lock();
        m_PostIDs.clear();
        m_WriteLock.unlock();
    }

    public void addPost(String postID)
    {
        m_WriteLock.lock();
        m_PostIDs.add(postID);
        m_PendingPostUpdate.add(m_PostIDs.size() - 1);
        updateUIOnPostChanged();
        m_WriteLock.unlock();

        addPostServer(postID);
    }

    private void addPostServer(String postID)
    {
        Post post = PostManager.getInstance().getPost(postID);
        if (post != null)
        {
            m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayUnion(post.m_DocRef))
                    .addOnFailureListener(e -> AlbumManager.getInstance().onAlbumError(m_AlbumID, postID, AlbumError.UPDATE_POSTS));
        }
    }

    public void removePost(String postID)
    {
        m_WriteLock.lock();
        for (int i = 0; i < m_PostIDs.size(); i++)
        {
            if (postID.equals(m_PostIDs.get(i)))
            {
                m_PostIDs.remove(postID);
                if (m_CurrPostIndex == i)
                {
                    if (m_PostIDs.isEmpty())
                    {
                        m_CurrPostIndex = 0;
                    } else
                    {
                        updatePostIndex(true);
                    }
                }
                updateUIOnPostChanged();
                m_ShouldUpdateTexture = true;
                break;
            }
        }
        m_TimeModified = Timestamp.now();
        m_WriteLock.unlock();

        Post post = PostManager.getInstance().removePost(postID);
        if (post != null)
        {
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            m_DocRef.update(AlbumConstants.TIME_MODIFIED, m_TimeModified);
            m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(post.m_DocRef));
            batch.commit();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (getClass() != obj.getClass()) return false;
        Album otherAlbum = (Album) obj;
        return m_AlbumID.equals(otherAlbum.m_AlbumID);
    }

    @Override
    public int hashCode()
    {
        return m_AlbumID.hashCode();
    }

    @NonNull
    @Override
    public String toString()
    {
        return "Album{" +
                "m_Posts=" + m_PostIDs +
                ", m_AlbumID='" + m_AlbumID + '\'' +
                ", m_GeoHash='" + m_GeoHash + '\'' +
                ", m_GeoPoint=" + m_GeoPoint +
                '}';
    }

    public boolean shouldUpdateTexture()
    {
        if (m_PendingPostUpdate.contains(m_CurrPostIndex))
        {
            Post post = PostManager.getInstance().getPost(getCurrentPostID());
            if (post != null && post.m_IsImageLoaded)
            {
                m_PendingPostUpdate.remove(m_CurrPostIndex);
            }
            return true;
        }

        boolean shouldUpdate = m_ShouldUpdateTexture;
        m_ShouldUpdateTexture = false;
        return shouldUpdate;
    }

    public String getCurrentPostID()
    {
        m_ReadLock.lock();
        if (!m_PostIDs.isEmpty() && m_CurrPostIndex < m_PostIDs.size())
        {
            m_ReadLock.unlock();
            return m_PostIDs.get(m_CurrPostIndex);
        }
        m_ReadLock.unlock();
        return null;
    }

    public void userInteractUpdatePostIndex(boolean next)
    {
        m_ReadLock.lock();
        updatePostIndex(next);
        m_ShouldUpdateTexture = true;
        m_ReadLock.unlock();
    }

    private void updatePostIndex(boolean next)
    {
        if (next) m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_PostIDs.size());
        else m_CurrPostIndex = mod(m_CurrPostIndex - 1, m_PostIDs.size());
    }

    public void updateUIOnPostChanged()
    {
        int activeIndex = m_CurrPostIndex;
        int size = m_PostIDs.size();

        float xOffset = 75.0f;
        m_UICanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float ratio = ((float) m_CanvasWidth / (float) m_CanvasHeight) - 1.0f;
        float xRootPos = m_CanvasWidth / 2.0f;
        float yRootPos = (m_CanvasHeight * 0.90f) / ratio;

        if (size - activeIndex == size)
        {
            drawSolidDot(xRootPos, yRootPos);

            int remaining = Math.min(3, size) - 1;
            for (int i = 0; i <= remaining; i++)
            {
                drawClearDot(xRootPos + xOffset * i + 1, yRootPos);
            }
        } else if (size - activeIndex == 1)
        {
            int remaining = Math.min(3, size) - 1;
            for (int i = remaining; i >= 0; i--)
            {
                drawClearDot(xRootPos - xOffset * i + 1, yRootPos);
            }

            drawSolidDot(xRootPos, yRootPos);
        } else
        {
            drawClearDot(xRootPos - xOffset, yRootPos);
            drawSolidDot(xRootPos, yRootPos);
            drawClearDot(xRootPos + xOffset, yRootPos);
        }

        if (m_CurrPostIndex < m_PostIDs.size()) {
            Post post = PostManager.getInstance().getPost(m_PostIDs.get(m_CurrPostIndex));
            if (post != null) {
                if (post.isLikedByCurrentUser()) {
                    drawHeart();
                }
            }
        }
    }

    private void drawSolidDot(float x, float y)
    {
        m_Paint.setARGB(255, 255, 255, 255);
        m_Paint.setStyle(Paint.Style.FILL);
        m_UICanvas.drawCircle(x, y, 25.0f, m_Paint);
    }

    private void drawClearDot(float x, float y)
    {
        m_Paint.setARGB(255, 255, 255, 255);
        m_Paint.setStrokeWidth(5);
        m_Paint.setStyle(Paint.Style.STROKE);
        m_UICanvas.drawCircle(x, y, 25.0f, m_Paint);
    }

    private void drawHeart()
    {
        m_Paint.setARGB(255, 200, 0,0);
        m_Paint.setStyle(Paint.Style.FILL);
        m_UICanvas.save();
        m_UICanvas.translate(35, 25);
        m_UICanvas.drawPath(m_HeartPath, m_Paint);
        m_UICanvas.restore();
    }

    private Path createHeartPath() {
        Path path = new Path();

        float width = 100.0f;
        float height = 100.0f;

        float pX = width/2f;
        float pY = (height/100f)*33.33f;

        float x1 = (width/100f)*50;
        float y1 = (height/100f)*5;
        float x2 = (width/100f)*90;
        float y2 = (height/100f)*10;
        float x3 = (width/100f)*90;
        float y3 = (height/100f)*33.33f;

        path.moveTo(pX,pY);
        path.cubicTo(x1, y1, x2, y2, x3, y3);
        path.moveTo(x3,pY);

        x1 = (width/100f)*90;
        y1 = (height/100f)*55f;
        x2 = (width/100f)*65;
        y2 = (height/100f)*60f;
        x3 = (width/100f)*50;
        y3 = (height/100f)*90f;

        path.cubicTo(x1, y1, x2, y2, x3, y3);
        path.lineTo(pX,pY);


        x1 = (width/100f)*50;
        y1 = (height/100f)*5;
        x2 = (width/100f)*10;
        y2 = (height/100f)*10;
        x3 = (width/100f)*10;
        y3 = (height/100f)*33.33f;

        path.moveTo(pX,pY);
        path.cubicTo(x1, y1, x2, y2, x3, y3);
        path.moveTo(x3,pY);

        x1 = (width/100f)*10;
        y1 = (height/100f)*55f;
        x2 = (width/100f)*35f;
        y2 = (height/100f)*60f;
        x3 = (width/100f)*50f;
        y3 = (height/100f)*90f;

        path.cubicTo(x1, y1, x2, y2, x3, y3);
        path.lineTo(pX,pY);

        path.moveTo(x3,y3);
        path.close();

        return path;
    }

    public Bitmap getUIBitmap()
    {
        return m_AlbumUI;
    }

    class PostUpdateIndexTask extends TimerTask
    {
        @Override
        public void run()
        {
            m_ReadLock.lock();
            if (m_PostIDs.isEmpty())
            {
                m_CurrPostIndex = 0;
                m_ShouldUpdateTexture = true;
                m_ReadLock.unlock();
                return;
            }
            updatePostIndex(true);
            updateUIOnPostChanged();
            m_ShouldUpdateTexture = true;
            m_ReadLock.unlock();
        }
    }

    public void stopAutoPostUpdateIndex()
    {
        m_PostUpdateTimerTask.cancel();
    }

    public void startAutoPostUpdateIndex()
    {
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
    }
}
