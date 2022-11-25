package com.perception.persep.firebase_helpers;


import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseGeoqueries
{
    private static final String TAG = "FirebaseGeoqueries";

    private static FirebaseGeoqueries m_Instance = null;
    private static int MAX_QUERY_COUNT = 10;

    public interface GeoqueryListener
    {
        void OnQueryResult(boolean successful, String message, List<Album> albums);
    }

    public GeoqueryListener m_Listener = null;

    // Database
    private final FirebaseFirestore m_FirebaseDB;

    public FirebaseGeoqueries()
    {
        m_FirebaseDB = FirebaseFirestore.getInstance();
    }

    public static FirebaseGeoqueries getInstance()
    {
        if (m_Instance == null) m_Instance = new FirebaseGeoqueries();
        return m_Instance;
    }

    public void AddAlbum(Album album)
    {
        // add album
        {
            String geohash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(album.m_Lat, album.m_Lon));

            Map<String, Object> updates = new HashMap<>(album.getMap());
            updates.put("geohash", geohash);

            DocumentReference albumRef = m_FirebaseDB.collection("albums").document(geohash);
            albumRef.update(updates);
        }

        // add posts
        {
            for(Post post : album.GetPosts())
            {
                String postID = post.getPostID().toString();
                DocumentReference postRef = m_FirebaseDB.collection("posts").document(postID);
                postRef.update(post.getMap());
            }
        }

        // update and link up data
        {

        }
    }

    private List<DocumentSnapshot> OnQueryAlbums(List<Task<QuerySnapshot>> tasks, GeoLocation queryCenter, double queryRadius)
    {
        Log.d(TAG, "===== OnQueryAlbums ====");
        List<DocumentSnapshot> albumDocSnapshots = new ArrayList<>();

        for (Task<QuerySnapshot> task : tasks)
        {
            QuerySnapshot snap = task.getResult();
            for (DocumentSnapshot doc : snap.getDocuments())
            {
                double docLat = doc.getDouble("lat");
                double docLon = doc.getDouble("lon");

                // filter out a few false positives due to GeoHash accuracy
                GeoLocation docLocation = new GeoLocation(docLat, docLon);
                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, queryCenter);
                if (distanceInM <= queryRadius)
                {
                    albumDocSnapshots.add(doc);
                }
            }
        }

        return albumDocSnapshots;
    }

    private List<DocumentSnapshot> OnQueryPosts(List<Task<DocumentSnapshot>> tasks)
    {
        Log.d(TAG, "===== OnQueryAlbums ====");
        List<DocumentSnapshot> postDocSnapshots = new ArrayList<>();

        for (Task<DocumentSnapshot> task : tasks)
        {
            postDocSnapshots.add(task.getResult());
        }

        return postDocSnapshots;
    }

    private List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter, double queryRadius, List<String> ignoreAlbums)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, queryRadius);
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = m_FirebaseDB.collection("albums")
                    .whereNotIn("geohash", ignoreAlbums)
                    .orderBy("geohash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .limit(MAX_QUERY_COUNT);

            tasks.add(query.get());
        }
        return tasks;
    }

    private List<Task<DocumentSnapshot>> GetPostQueryTasks(List<DocumentSnapshot> albumDocs)
    {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>(albumDocs.size());

        for(DocumentSnapshot albumDoc : albumDocs)
        {
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get("posts");
            for(DocumentReference postRef : postRefs)
            {
                tasks.add(postRef.get());
            }
        }

        return tasks;
    }

    private List<Album> GetConvertedData(List<DocumentSnapshot> albumDocs, List<DocumentSnapshot> postDocs)
    {
        List<Album> albums = new ArrayList<>();

        for(DocumentSnapshot albumDoc : albumDocs)
        {
            String albumID = albumDoc.getId();

            Album album = new Album(albumDoc.getData());
            for(DocumentSnapshot postDoc : postDocs)
            {
                String postAlbumID = postDoc.getString("album_id");
                if (albumID.equals(postAlbumID))
                {
                    album.AddPost(new Post(postDoc.getData()));
                }
            }
            albums.add(album);
        }

        return albums;
    }

    public void QueryNearbyAlbums(double lat, double lon, double queryRadius, List<String> ignoreAlbums)
    {
        if (m_Listener == null)
        {
            return;
        }

        Log.d(TAG, "===== QueryNearbyAlbums ====");

        GeoLocation queryCenter = new GeoLocation(lat, lon);
        List<Task<QuerySnapshot>> albumQueryTasks = GetAlbumQueryTasks(queryCenter, queryRadius, ignoreAlbums);
        Tasks.whenAllSuccess(albumQueryTasks).addOnCompleteListener(albumsResult -> {
            List<DocumentSnapshot> albumDocs = OnQueryAlbums(albumQueryTasks, queryCenter, queryRadius);

            List<Task<DocumentSnapshot>> postQueryTasks = GetPostQueryTasks(albumDocs);
            Tasks.whenAllComplete(postQueryTasks).addOnCompleteListener(postsResult -> {
                List<DocumentSnapshot> postDocs = OnQueryPosts(postQueryTasks);

                List<Album> albumData = GetConvertedData(albumDocs, postDocs);
                m_Listener.OnQueryResult(true, "", albumData);
            })
            .addOnFailureListener(e -> {
                m_Listener.OnQueryResult(false, "Query posts failed", null);
            });
        })
        .addOnFailureListener(e -> {
            m_Listener.OnQueryResult(false, "Query albums failed", null);
        });
    }

    private void PrintData(List<DocumentSnapshot> albumDocs, List<DocumentSnapshot> postDocs)
    {
        Log.d(TAG, "===== Album Docs =====");
        String[] ALBUM_TAGS = { "geohash", "lat", "lon", "posts" };
        for (DocumentSnapshot albumDoc : albumDocs)
        {
            for(String tag : ALBUM_TAGS)
            {
                Log.d(TAG, tag + ": " + albumDoc.get(tag).toString());
            }
        }

        Log.d(TAG, "===== Post Docs =====");
        String[] POST_TAGS = { "imageuri", "user" };
        for (DocumentSnapshot postDoc : postDocs)
        {
            for(String tag : POST_TAGS)
            {
                Log.d(TAG, tag + ": " + postDoc.get(tag).toString());
            }
        }
    }
}
