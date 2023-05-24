package com.perception.persep.firebase_helpers;


import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;

import java.util.ArrayList;
import java.util.List;

public class FirebaseQueries
{
    private static final String QUERY_NEARBY_TAG = "QueryGeoqueries";

    static public List<Task<QuerySnapshot>> GetAlbumQueryTasks(GeoLocation queryCenter, double maxQueryRadius)
    {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        List<GeoQueryBounds> queryBounds = GeoFireUtils.getGeoHashQueryBounds(queryCenter, maxQueryRadius);
        for (GeoQueryBounds bound : queryBounds)
        {
            Query query = FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME);
            query = query.orderBy(AlbumConstants.GEO_HASH)
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .limit(QueryConstants.MAX_QUERY_COUNT);
            tasks.add(query.get());
        }
        return tasks;
    }

    static public List<DocumentSnapshot> OnAlbumQueryTasks(List<Object> albumSnapshots, GeoLocation queryCenter, double maxQueryRadius)
    {
        Log.d(QUERY_NEARBY_TAG, "===== OnQueryAlbums ====");
        List<DocumentSnapshot> albumDocSnapshots = new ArrayList<>();

        for (Object albumSnapshot : albumSnapshots)
        {
            if (!(albumSnapshot instanceof QuerySnapshot)) continue;

            QuerySnapshot snapshot = (QuerySnapshot) albumSnapshot;
            for (DocumentSnapshot albumDoc : snapshot.getDocuments())
            {
                GeoPoint geoPoint = (GeoPoint) albumDoc.get(PostConstants.GEO_POINT);
                GeoLocation docLocation = new GeoLocation(geoPoint.getLatitude(), geoPoint.getLongitude());
                double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, queryCenter);
                if (distanceInM < maxQueryRadius)
                {
                    albumDocSnapshots.add(albumDoc);
                }
            }
        }

        return albumDocSnapshots;
    }
}
