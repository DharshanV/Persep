package com.perception.persep.firebase_helpers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.models.User;

import java.util.Objects;

public class FirebaseUserAuthentication
{
    private static final String TAG = "UserAuthentication";

    private static FirebaseUserAuthentication m_Instance = null;

    public interface UserRegisterAuthListener  {
        void WasRegisterSuccessful(boolean successful, String message);
    }

    public interface UserLoginAuthListener  {
        void WasLoginSuccessful(boolean successful, User user, String message);
    }

    // Authentication
    private FirebaseAuth m_FirebaseAuth;
    public UserRegisterAuthListener m_UserRegisterAuthListener;
    public UserLoginAuthListener m_UserLoginAuthListener;

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

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    public void RegisterNewUser(String username, String email, String password)
    {
        Log.d(TAG, "Creating user with email: '" + email);
        m_User = new User();
        m_User.m_Email = email;
        m_User.m_Username = username;

        m_FirebaseAuth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener(authResult -> {
            Log.d(TAG, "Register Auth successful");
            FirebaseUser currentUser = authResult.getUser();
            m_User.m_UserID = currentUser.getUid();
            TryAddUserData();
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Login Auth error: " + e);
            m_UserRegisterAuthListener.WasRegisterSuccessful(false, "User authentication failed");
        })
        .addOnCanceledListener(() -> {
            Log.d(TAG, "Login Auth canceled");
            m_UserRegisterAuthListener.WasRegisterSuccessful(false, "User authentication failed");
        });
    }

    public void LoginUser(String email, String password)
    {
        Log.d(TAG, "Login in user with email: '" + email);
        m_FirebaseAuth.signInWithEmailAndPassword(email, password)
        .addOnSuccessListener(authResult -> {
            Log.d(TAG, "Login Auth successful");
            FirebaseUser firebaseUser = authResult.getUser();
            TryGetUserData(firebaseUser.getUid());
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Login Auth error: " + e);
            m_UserLoginAuthListener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        })
        .addOnCanceledListener(() -> {
            Log.d(TAG, "Login Auth canceled");
            m_UserLoginAuthListener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        });
    }

    private void TryGetUserData(String uid)
    {
        Log.d(TAG, "Trying to get user data...");
        DocumentReference docRef = m_FirebaseDB.collection("users").document(uid);
        docRef.get()
        .addOnSuccessListener(documentSnapshot -> {
            Log.d(TAG, "Successfully retrieved user data");
            User user = new User(Objects.requireNonNull(documentSnapshot.getData()));
            m_UserLoginAuthListener.WasLoginSuccessful(true, user, "");
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Get user data from DB failed" + e);
            m_UserLoginAuthListener.WasLoginSuccessful(false, null, "Couldn't sign in");
        })
        .addOnCanceledListener(() -> {
            Log.d(TAG, "Get user data from DB canceled");
            m_UserLoginAuthListener.WasLoginSuccessful(false, null, "Couldn't sign in");
        });
    }

    private void TryAddUserData()
    {
        Log.d(TAG, "Trying to add new user...");
        DocumentReference userDoc = m_FirebaseDB.collection("users").document(m_User.m_UserID);
        userDoc.set(m_User.getMap())
        .addOnSuccessListener(unused -> {
            Log.d(TAG, "Successfully created user data");
            m_UserRegisterAuthListener.WasRegisterSuccessful(true, "");
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Write user data to DB failed: " + e);
            m_UserRegisterAuthListener.WasRegisterSuccessful(false, "Couldn't create user data");
        })
        .addOnCanceledListener(() -> {
            Log.d(TAG, "Write user data to DB canceled");
            m_UserRegisterAuthListener.WasRegisterSuccessful(false, "Couldn't create user data");
        });
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
        m_FirebaseDB = FirebaseFirestore.getInstance();
    }
}
