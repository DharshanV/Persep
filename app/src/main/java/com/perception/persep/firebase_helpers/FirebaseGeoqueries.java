package com.perception.persep.firebase_helpers;


import android.graphics.Bitmap;

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
import java.util.Objects;

public class FirebaseGeoqueries
{
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
            for(Post post : album.m_Posts)
            {
                String postID = post.getUniqueID().toString();
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

    private List<DocumentSnapshot> OnQueryPosts(List<Task<QuerySnapshot>> tasks, List<DocumentSnapshot> albumDocs)
    {
        List<DocumentSnapshot> postDocSnapshots = new ArrayList<>();

        for (Task<QuerySnapshot> task : tasks)
        {
            QuerySnapshot snap = task.getResult();
            postDocSnapshots.addAll(snap.getDocuments());
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

    private List<Task<QuerySnapshot>> GetPostQueryTasks(List<DocumentSnapshot> albumDocs)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        return tasks;
    }

    private List<Task<QuerySnapshot>> GetImageQueryTasks(List<DocumentSnapshot> postDocs)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        return tasks;
    }


    public void QueryNearbyAlbums(double lat, double lon, double queryRadius, List<String> ignoreAlbums)
    {
        if (m_Listener == null)
        {
            return;
        }

        GeoLocation queryCenter = new GeoLocation(lat, lon);
        List<Task<QuerySnapshot>> albumQueryTasks = GetAlbumQueryTasks(queryCenter, queryRadius, ignoreAlbums);
        Tasks.whenAllComplete(albumQueryTasks).addOnCompleteListener(albumsResult -> {
            List<DocumentSnapshot> albumDocs = OnQueryAlbums(albumQueryTasks, queryCenter, queryRadius);

            List<Task<QuerySnapshot>> postQueryTasks = GetPostQueryTasks(albumDocs);
            Tasks.whenAllComplete(postQueryTasks).addOnCompleteListener(postsResult -> {
                List<DocumentSnapshot> postDocs = OnQueryPosts(postQueryTasks, albumDocs);

                List<Task<QuerySnapshot>> imageQueryTasks = GetImageQueryTasks(postDocs);
                Tasks.whenAllComplete(imageQueryTasks).addOnCompleteListener(imagesResult -> {
//                    List<Bitmap> images = OnQueryImages(imageQueryTasks);
//
//                    List<Album> albums = ParseQueriedData(albumDocs, postDocs, images);
//                    m_Listener.OnQueryResult(true, "", albums);
                });
            });
        });
    }
}
