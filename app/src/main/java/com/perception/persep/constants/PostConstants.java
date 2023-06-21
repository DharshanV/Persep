package com.perception.persep.constants;

public class PostConstants
{
    public static final String COLLECTION_NAME = "posts";
    public static final String ALBUM_ID = "album_id";
    public static final String IMAGE_URI = "image_uri";
    public static final String USER = "user";
    public static final String LIKES = "likes";
    public static final String USERS_LIKED = "users_liked";
    public static final String GEO_POINT = "geo_point";
    public static final String GEO_LOCATION = "geo_location";
    public static final String TIME_CREATED = "time_created";
    public static final String TIME_MODIFIED = "time_modified";
    public static final String COMMENTS = "comments";

    public static final String[] REQUIRED_TAGS = {ALBUM_ID, IMAGE_URI, LIKES, GEO_POINT, GEO_LOCATION, TIME_CREATED, TIME_MODIFIED};

    public static final int IMAGE_WIDTH = 1280;
    public static final int IMAGE_HEIGHT = 720;
}
