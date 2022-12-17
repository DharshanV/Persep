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
import java.util.List;

public class FirebaseGeoqueries
{
    private static final String QUERY_NEARBY_TAG = "QueryGeoqueries";
    private static final String ADD_NEARBY_TAG = "AddGeoqueries";

    private static FirebaseGeoqueries m_Instance = null;
    private static int MAX_QUERY_COUNT = 10;
    private static float MAX_QUERY_RADIUS = 100.0f;

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

    public void AddPost(Post post, double lat, double lon)
    {
        Log.d(ADD_NEARBY_TAG, "===== AddPost =====");
        GeoLocation postLocation = new GeoLocation(lat, lon);

        List<Task<QuerySnapshot>> queryAlbumTasks = GetAlbumQueryTasks(postLocation);

        Tasks.whenAllComplete(queryAlbumTasks).addOnCompleteListener(t -> {
            DocumentSnapshot closestAlbum = OnQueryClosestAlbum(queryAlbumTasks, postLocation);

            if (closestAlbum == null) {
                Log.d(ADD_NEARBY_TAG, "Didn't find any nearby album. Creating a new one...");
            }
            else {
                Log.d(ADD_NEARBY_TAG, "Found nearby albums, finding closest one to add...");
            }
        }).addOnFailureListener(e -> { m_Listener.OnQueryResult(false, "Add post query failed", null); });
    }

    private void AddPostToAlbum(DocumentSnapshot albumDoc, Post post)
    {
        String postID = post.getPostID().toString();
        DocumentReference postRef = m_FirebaseDB.collection("posts").document(postID);
        postRef.update(post.getMap()).addOnSuccessListener(unused -> {

        });
    }

    private List<DocumentSnapshot> OnQueryAlbums(List<Task<QuerySnapshot>> tasks, GeoLocation queryCenter)
    {
        Log.d(QUERY_NEARBY_TAG, "===== OnQueryAlbums ====");
        List<DocumentSnapshot> albumDocSnapshots = new ArrayList<>();

        for (Task<QuerySnapshot> task : tasks)
        {
            QuerySnapshot snap = task.getResult();
            for (DocumentSnapshot doc : snap.getDocuments())
            {
                double docLat = doc.getDouble("lat");
                double docLon = doc.getDouble("lon");

                GeoLocation docLocation = new GeoLocation(docLat, docLon);
                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, queryCenter);
                if (distanceInM < MAX_QUERY_RADIUS)
                {
                    albumDocSnapshots.add(doc);
                }
            }
        }

        return albumDocSnapshots;
    }

    private DocumentSnapshot OnQueryClosestAlbum(List<Task<QuerySnapshot>> tasks, GeoLocation queryCenter)
    {
        double closestDist = Double.MAX_VALUE;
        DocumentSnapshot closestAlbumDoc = null;

        for (Task<QuerySnapshot> task : tasks)
        {
            QuerySnapshot snap = task.getResult();
            for (DocumentSnapshot albumDoc : snap.getDocuments())
            {
                double lat = albumDoc.getDouble("lat");
                double lon = albumDoc.getDouble("lon");

                GeoLocation albumLocation = new GeoLocation(lat, lon);
                double distanceInM = GeoFireUtils.getDistanceBetween(albumLocation, queryCenter);
                if (distanceInM < MAX_QUERY_RADIUS && distanceInM < closestDist)
                {
                    closestAlbumDoc = albumDoc;
                    closestDist = distanceInM;
                }
            }
        }
        return closestAlbumDoc;
    }

    private List<DocumentSnapshot> OnQueryPosts(List<Task<DocumentSnapshot>> tasks)
    {
        Log.d(QUERY_NEARBY_TAG, "===== OnQueryPosts ====");
        List<DocumentSnapshot> postDocSnapshots = new ArrayList<>();

        for (Task<DocumentSnapshot> task : tasks)
        {
            postDocSnapshots.add(task.getResult());
        }

        return postDocSnapshots;
    }

    private List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter, List<String> ignoreAlbums)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, MAX_QUERY_RADIUS);
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

    private List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, MAX_QUERY_RADIUS);
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = m_FirebaseDB.collection("albums")
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

    public void QueryNearbyAlbums(double lat, double lon, List<String> ignoreAlbums)
    {
        if (m_Listener == null)
        {
            return;
        }

        Log.d(QUERY_NEARBY_TAG, "===== QueryNearbyAlbums ====");

        GeoLocation queryCenter = new GeoLocation(lat, lon);
        List<Task<QuerySnapshot>> albumQueryTasks = GetAlbumQueryTasks(queryCenter, ignoreAlbums);
        Tasks.whenAllSuccess(albumQueryTasks).addOnCompleteListener(albumsResult -> {
            List<DocumentSnapshot> albumDocs = OnQueryAlbums(albumQueryTasks, queryCenter);

            List<Task<DocumentSnapshot>> postQueryTasks = GetPostQueryTasks(albumDocs);
            Tasks.whenAllComplete(postQueryTasks).addOnCompleteListener(postsResult -> {
                List<DocumentSnapshot> postDocs = OnQueryPosts(postQueryTasks);

//                List<Album> albumData = GetConvertedData(albumDocs, postDocs);
//                m_Listener.OnQueryResult(true, "", albumData);
                PrintData(albumDocs, postDocs);
                m_Listener.OnQueryResult(false, "", null);
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
        Log.d(QUERY_NEARBY_TAG, "===== Album Docs =====");
        String[] ALBUM_TAGS = { "geohash", "lat", "lon", "posts" };
        for (DocumentSnapshot albumDoc : albumDocs)
        {
            for(String tag : ALBUM_TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + albumDoc.get(tag).toString());
            }
        }

        Log.d(QUERY_NEARBY_TAG, "===== Post Docs =====");
        String[] POST_TAGS = { "album_id", "image_uri", "user" };
        for (DocumentSnapshot postDoc : postDocs)
        {
            for(String tag : POST_TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + postDoc.get(tag).toString());
            }
        }
    }
}
