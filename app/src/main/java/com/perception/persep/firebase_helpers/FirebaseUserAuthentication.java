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
        m_User = new User();
        m_User.m_Email = email;
        m_User.m_Username = username;

        m_FirebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            FirebaseUser currentUser = authResult.getUser();
            m_User.m_UserID = currentUser.getUid();
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();
            profileUpdates.setDisplayName(username);
            currentUser.updateProfile(profileUpdates.build());
            TryAddUserData(listener);
        }).addOnFailureListener(e -> {
            listener.WasRegisterSuccessful(false, "User authentication failed");
        }).addOnCanceledListener(() -> {
            listener.WasRegisterSuccessful(false, "User authentication failed");
        });
    }

    public void LoginUser(String email, String password, UserLoginAuthListener listener)
    {
        m_FirebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            FirebaseUser firebaseUser = authResult.getUser();
            TryGetUserData(firebaseUser.getUid(), listener);
        }).addOnFailureListener(e -> {
            listener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        }).addOnCanceledListener(() -> {
            listener.WasLoginSuccessful(false, null, "Couldn't sign in...");
        });
    }

    private void TryGetUserData(String uid, UserLoginAuthListener listener)
    {
        DocumentReference docRef = m_FirebaseDB.collection("users").document(uid);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            User user = new User(Objects.requireNonNull(documentSnapshot.getData()));
            listener.WasLoginSuccessful(true, user, "");
        }).addOnFailureListener(e -> {
            listener.WasLoginSuccessful(false, null, "Couldn't sign in");
        }).addOnCanceledListener(() -> {
            listener.WasLoginSuccessful(false, null, "Couldn't sign in");
        });
    }

    private void TryAddUserData(UserRegisterAuthListener listener)
    {
        DocumentReference userDoc = m_FirebaseDB.collection("users").document(m_User.m_UserID);
        userDoc.set(m_User.getMap()).addOnSuccessListener(unused -> {
            listener.WasRegisterSuccessful(true, "");
        }).addOnFailureListener(e -> {
            listener.WasRegisterSuccessful(false, "Couldn't create user data");
        }).addOnCanceledListener(() -> {
            listener.WasRegisterSuccessful(false, "Couldn't create user data");
        });
    }

    private void SetupFirebaseAuth()
    {
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
