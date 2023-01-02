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
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;

import java.util.ArrayList;
import java.util.HashMap;
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
//        Log.d(ADD_NEARBY_TAG, "===== AddPost =====");
//        GeoLocation postLocation = new GeoLocation(lat, lon);
//
//        List<Task<QuerySnapshot>> queryAlbumTasks = GetAlbumQueryTasks(postLocation);
//
//        Tasks.whenAllComplete(queryAlbumTasks).addOnCompleteListener(t -> {
//            DocumentSnapshot closestAlbum = OnQueryClosestAlbum(queryAlbumTasks, postLocation);
//
//            if (closestAlbum == null) {
//                Log.d(ADD_NEARBY_TAG, "No nearby album, creating a new one...");
//                Map<String, Object> data = new HashMap<>();
//
//                m_FirebaseDB.collection(AlbumConstants.COLLECTION_NAME).add(data).addOnSuccessListener(documentReference -> {
//                }).addOnFailureListener(e -> { });
//            }
//            else {
//                Log.d(ADD_NEARBY_TAG, "Found an album, adding post to it...");
//                AddPostToAlbum(closestAlbum, post);
//            }
//        }).addOnFailureListener(e -> { });
    }

    private void AddPostToAlbum(DocumentSnapshot albumDoc, Post post)
    {
        String postID = post.getPostID().toString();
        DocumentReference postRef = m_FirebaseDB.collection(PostConstants.COLLECTION_NAME).document(postID);
        postRef.set(post.getMap()).addOnSuccessListener(unused -> {

        });
    }

    private List<DocumentSnapshot> OnQueryAlbums(List<Object> albumSnapshots, GeoLocation queryCenter)
    {
        Log.d(QUERY_NEARBY_TAG, "===== OnQueryAlbums ====");
        List<DocumentSnapshot> albumDocSnapshots = new ArrayList<>();

        for (Object albumSnapshot : albumSnapshots)
        {
            if (!(albumSnapshot instanceof QuerySnapshot)) continue;

            QuerySnapshot snapshot = (QuerySnapshot) albumSnapshot;
            for (DocumentSnapshot albumDoc : snapshot.getDocuments())
            {
                double docLat = albumDoc.getDouble(AlbumConstants.LAT);
                double docLon = albumDoc.getDouble(AlbumConstants.LON);

                GeoLocation docLocation = new GeoLocation(docLat, docLon);
                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, queryCenter);
                if (distanceInM < MAX_QUERY_RADIUS)
                {
                    albumDocSnapshots.add(albumDoc);
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
                double lat = albumDoc.getDouble(AlbumConstants.LAT);
                double lon = albumDoc.getDouble(AlbumConstants.LON);

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

    private List<DocumentSnapshot> OnQueryPosts(List<Object> postSnapshots)
    {
        Log.d(QUERY_NEARBY_TAG, "===== OnQueryPosts ====");
        List<DocumentSnapshot> postDocSnapshots = new ArrayList<>();

        for (Object postSnapshot : postSnapshots)
        {
            if (!(postSnapshot instanceof DocumentSnapshot)) continue;

            postDocSnapshots.add((DocumentSnapshot) postSnapshot);
        }

        return postDocSnapshots;
    }

    private List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter, List<String> ignoreAlbums)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, MAX_QUERY_RADIUS);
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = m_FirebaseDB.collection(AlbumConstants.COLLECTION_NAME)
                    .whereNotIn(AlbumConstants.GEO_HASH, ignoreAlbums)
                    .orderBy(AlbumConstants.GEO_HASH)
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
            Query query = m_FirebaseDB.collection(AlbumConstants.COLLECTION_NAME)
                    .orderBy(AlbumConstants.GEO_HASH)
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
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
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

        HashMap<String, List<DocumentSnapshot>> mapAlbumIDToPost = new HashMap<>();
        for (DocumentSnapshot postDoc : postDocs)
        {
            String postAlbumID = postDoc.getString(PostConstants.ALBUM_ID);
            if (!mapAlbumIDToPost.containsKey(postAlbumID))
            {
                mapAlbumIDToPost.put(postAlbumID, new ArrayList<>());
            }
            mapAlbumIDToPost.get(postAlbumID).add(postDoc);
        }

        for(DocumentSnapshot albumDoc : albumDocs)
        {
            String albumID = albumDoc.getId();

            Album album = new Album(albumDoc.getData());
            List<DocumentSnapshot> albumPostDocs = mapAlbumIDToPost.get(albumID);
            assert albumPostDocs != null;
            for(DocumentSnapshot postDoc : albumPostDocs)
            {
                album.AddPost(new Post(postDoc.getData()));
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
        Tasks.whenAllSuccess(albumQueryTasks).addOnSuccessListener(albumsResult -> {
            List<DocumentSnapshot> albumDocs = OnQueryAlbums(albumsResult, queryCenter);

            List<Task<DocumentSnapshot>> postQueryTasks = GetPostQueryTasks(albumDocs);
            Tasks.whenAllSuccess(postQueryTasks).addOnSuccessListener(postsResult -> {
                List<DocumentSnapshot> postDocs = OnQueryPosts(postsResult);

//                List<Album> albumData = GetConvertedData(albumDocs, postDocs);
//                m_Listener.OnQueryResult(true, "", albumData);
//                PrintData(albumDocs, postDocs);
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
        for (DocumentSnapshot albumDoc : albumDocs)
        {
            for(String tag : AlbumConstants.TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + albumDoc.get(tag).toString());
            }
        }

        Log.d(QUERY_NEARBY_TAG, "===== Post Docs =====");
        for (DocumentSnapshot postDoc : postDocs)
        {
            for(String tag : PostConstants.TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + postDoc.get(tag).toString());
            }
        }
    }
}
