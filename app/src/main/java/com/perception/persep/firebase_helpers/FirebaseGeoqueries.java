package com.perception.persep.firebase_helpers;


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

    public void QueryNearbyAlbums(double lat, double lon, double queryRadius)
    {
        if (m_Listener == null)
        {
            return;
        }

        GeoLocation queryCenter = new GeoLocation(lat, lon);
        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, queryRadius);

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = m_FirebaseDB.collection("albums")
                    .orderBy("geohash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .limit(MAX_QUERY_COUNT);

            tasks.add(query.get());
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(t -> {
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

                    List<Album> albums = new ArrayList<>();
                    for (DocumentSnapshot albumDocSnap : albumDocSnapshots)
                    {
                        albums.add(new Album(Objects.requireNonNull(albumDocSnap.getData())));
                    }
                    m_Listener.OnQueryResult(true, "", albums);
                });
    }
}
