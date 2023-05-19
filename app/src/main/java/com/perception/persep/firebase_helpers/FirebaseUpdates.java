package com.perception.persep.firebase_helpers;

import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.constants.UserConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseUpdates
{
    private static final String ADD_NEARBY_TAG = "FirebaseUpdates";
}
