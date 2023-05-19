package com.perception.persep.constants;

import com.google.firebase.firestore.GeoPoint;

public class AlbumConstants
{
    public static final String COLLECTION_NAME = "albums";
    public static final String GEO_HASH = "geo_hash";
    public static final String GEO_POINT = "geo_point";
    public static final String LAT = "lat";
    public static final String LON = "lon";
    public static final String POSTS = "posts";
    public static final String TIME_CREATED = "time_created";
    public static final String TIME_MODIFIED = "time_modified";

    public static final String[] TAGS = {GEO_HASH, LAT, LON, POSTS, TIME_CREATED, TIME_MODIFIED};
}
