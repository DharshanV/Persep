package com.perception.persep.firebase_helpers;


import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FirebaseQueries
{
    private static final String QUERY_NEARBY_TAG = "QueryGeoqueries";

    private static FirebaseQueries m_Instance = null;

    public interface AlbumQueryListener
    {
        void OnQueryResult(boolean successful, String message, List<Album> albums);
    }

    public interface UserPostsQueryListener
    {
        void OnQueryResult(boolean successful, String message, List<Post> posts);
    }

    // Database
    private final FirebaseFirestore m_Firestore;

    public FirebaseQueries()
    {
        m_Firestore = FirebaseFirestore.getInstance();
    }

    public static FirebaseQueries getInstance()
    {
        if (m_Instance == null) m_Instance = new FirebaseQueries();
        return m_Instance;
    }

    private List<DocumentSnapshot> OnQueryAlbums(List<Object> albumSnapshots, GeoLocation queryCenter, double maxQueryRadius)
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
                if (distanceInM < maxQueryRadius)
                {
                    albumDocSnapshots.add(albumDoc);
                }
            }
        }

        return albumDocSnapshots;
    }

    public DocumentSnapshot OnQueryClosestAlbum(List<Task<QuerySnapshot>> tasks, GeoLocation queryCenter, double maxQueryRadius)
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
                if (distanceInM < maxQueryRadius && distanceInM < closestDist)
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

    public List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter, List<String> ignoreAlbums, double maxQueryRadius)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, maxQueryRadius);
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = m_Firestore.collection(AlbumConstants.COLLECTION_NAME);
            if (ignoreAlbums != null && !ignoreAlbums.isEmpty())
            {
                Log.d(QUERY_NEARBY_TAG, "Ignore albums: " + ignoreAlbums.toString());
                query = query.whereNotIn(AlbumConstants.GEO_HASH, ignoreAlbums);
            }

            query = query.orderBy(AlbumConstants.GEO_HASH)
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .limit(QueryConstants.MAX_QUERY_COUNT);
            tasks.add(query.get());
        }
        return tasks;
    }

    private List<Task<DocumentSnapshot>> GetPostQueryTasks(List<DocumentSnapshot> albumDocs)
    {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>(albumDocs.size());

        for (DocumentSnapshot albumDoc : albumDocs)
        {
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
            for (DocumentReference postRef : postRefs)
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

        for (DocumentSnapshot albumDoc : albumDocs)
        {
            String albumID = albumDoc.getId();

            Album album = new Album(albumDoc.getId(), albumDoc.getData());
            List<DocumentSnapshot> albumPostDocs = mapAlbumIDToPost.get(albumID);
            assert albumPostDocs != null;
            for (DocumentSnapshot postDoc : albumPostDocs)
            {
                album.AddPost(new Post(postDoc.getId(), postDoc.getData()));
            }
            albums.add(album);
        }

        return albums;
    }

    public void QueryNearbyAlbums(double lat, double lon, List<String> ignoreAlbums, AlbumQueryListener listener)
    {
        if (listener == null)
        {
            return;
        }

        Log.d(QUERY_NEARBY_TAG, "===== QueryNearbyAlbums ====");

        GeoLocation queryCenter = new GeoLocation(lat, lon);
        List<Task<QuerySnapshot>> albumQueryTasks = GetAlbumQueryTasks(queryCenter, ignoreAlbums, QueryConstants.MAX_QUERY_RADIUS);
        Tasks.whenAllSuccess(albumQueryTasks).addOnSuccessListener(albumsResult -> {
            List<DocumentSnapshot> albumDocs = OnQueryAlbums(albumsResult, queryCenter, QueryConstants.MAX_QUERY_RADIUS);

            if (albumDocs.isEmpty())
            {
                Log.d(QUERY_NEARBY_TAG, "Early exiting, no nearby albums found...");
                listener.OnQueryResult(true, "", Collections.emptyList());
                return;
            }

            List<Task<DocumentSnapshot>> postQueryTasks = GetPostQueryTasks(albumDocs);
            Tasks.whenAllSuccess(postQueryTasks).addOnSuccessListener(postsResult -> {
                List<DocumentSnapshot> postDocs = OnQueryPosts(postsResult);

                List<Album> albumData = GetConvertedData(albumDocs, postDocs);
                listener.OnQueryResult(true, "", albumData);
//                PrintData(albumDocs, postDocs);
//                m_Listener.OnQueryResult(false, "", null);
            }).addOnFailureListener(e ->
            {
                listener.OnQueryResult(false, "Query posts failed", null);
            });
        }).addOnFailureListener(e ->
        {
            listener.OnQueryResult(false, "Query albums failed", null);
        });
    }

    public void QueryUserPosts(UserPostsQueryListener listener)
    {

    }

    private void PrintData(List<DocumentSnapshot> albumDocs, List<DocumentSnapshot> postDocs)
    {
        Log.d(QUERY_NEARBY_TAG, "===== Album Docs =====");
        for (DocumentSnapshot albumDoc : albumDocs)
        {
            for (String tag : AlbumConstants.TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + albumDoc.get(tag).toString());
            }
        }

        Log.d(QUERY_NEARBY_TAG, "===== Post Docs =====");
        for (DocumentSnapshot postDoc : postDocs)
        {
            for (String tag : PostConstants.TAGS)
            {
                Log.d(QUERY_NEARBY_TAG, tag + ": " + postDoc.get(tag).toString());
            }
        }
    }

    public void AddAlbumListener(EventListener<QuerySnapshot> listener)
    {
        m_Firestore.collection(AlbumConstants.COLLECTION_NAME).addSnapshotListener(listener);
    }

    public void RemoveAlbumListener()
    {
        ListenerRegistration registration = m_Firestore.collection(AlbumConstants.COLLECTION_NAME).addSnapshotListener((value, error) -> { });
        registration.remove();
    }
}
