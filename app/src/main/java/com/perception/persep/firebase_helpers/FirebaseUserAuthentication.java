package com.perception.persep.firebase_helpers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.user_data.User;

import java.util.Objects;

public class FirebaseUserAuthentication
{
    private static final String TAG = "UserAuthentication";

    private static FirebaseUserAuthentication m_Instance = null;
    // Authentication
    private FirebaseAuth m_FirebaseAuth;
    // Database
    private FirebaseFirestore m_FirebaseDB;
    private User m_User;

    public FirebaseUserAuthentication()
    {
        SetupFirebaseAuth();
    }

    public static FirebaseUserAuthentication getInstance()
    {
        if (m_Instance == null) m_Instance = new FirebaseUserAuthentication();
        return m_Instance;
    }

    public void RegisterNewUser(String username, String email, String password, UserRegisterAuthListener listener)
    {
        Log.d(TAG, "Creating user with email: '" + email);
        m_User = new User();
        m_User.m_Email = email;
        m_User.m_Username = username;

        m_FirebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            Log.d(TAG, "Register Auth successful");
            FirebaseUser currentUser = authResult.getUser();
            m_User.m_UserID = currentUser.getUid();
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();
            profileUpdates.setDisplayName(username);
            currentUser.updateProfile(profileUpdates.build());
            TryAddUserData(listener);
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Login Auth error: " + e);
            listener.WasRegisterSuccessful(false, "User authentication failed");
        }).addOnCanceledListener(() -> {
            Log.d(TAG, "Login Auth canceled");
            listener.WasRegisterSuccessful(false, "User authentication failed");
        });
    }

    public void LoginUser(String email, String password, UserLoginAuthListener listener)
    {
        Log.d(TAG, "Login in user with email: '" + email);
        m_FirebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            Log.d(TAG, "Login Auth successful");
            FirebaseUser firebaseUser = authResult.getUser();
            TryGetUserData(firebaseUser.getUid(), listener);
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Login Auth error: " + e);
            listener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        }).addOnCanceledListener(() -> {
            Log.d(TAG, "Login Auth canceled");
            listener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        });
    }

    private void TryGetUserData(String uid, UserLoginAuthListener listener)
    {
        Log.d(TAG, "Trying to get user data...");
        DocumentReference docRef = m_FirebaseDB.collection("users").document(uid);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            Log.d(TAG, "Successfully retrieved user data");
            User user = new User(Objects.requireNonNull(documentSnapshot.getData()));
            listener.WasLoginSuccessful(true, user, "");
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Get user data from DB failed" + e);
            listener.WasLoginSuccessful(false, null, "Couldn't sign in");
        }).addOnCanceledListener(() -> {
            Log.d(TAG, "Get user data from DB canceled");
            listener.WasLoginSuccessful(false, null, "Couldn't sign in");
        });
    }

    private void TryAddUserData(UserRegisterAuthListener listener)
    {
        Log.d(TAG, "Trying to add new user...");
        DocumentReference userDoc = m_FirebaseDB.collection("users").document(m_User.m_UserID);
        userDoc.set(m_User.getMap()).addOnSuccessListener(unused -> {
            Log.d(TAG, "Successfully created user data");
            listener.WasRegisterSuccessful(true, "");
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Write user data to DB failed: " + e);
            listener.WasRegisterSuccessful(false, "Couldn't create user data");
        }).addOnCanceledListener(() -> {
            Log.d(TAG, "Write user data to DB canceled");
            listener.WasRegisterSuccessful(false, "Couldn't create user data");
        });
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
        m_FirebaseDB = FirebaseFirestore.getInstance();
    }

    public interface UserRegisterAuthListener
    {
        void WasRegisterSuccessful(boolean successful, String message);
    }

    public interface UserLoginAuthListener
    {
        void WasLoginSuccessful(boolean successful, User user, String message);
    }
}
