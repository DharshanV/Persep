package com.perception.persep.firebase_helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.models.User;

public class FirebaseUserAuthentication implements OnCompleteListener, OnSuccessListener, OnFailureListener, OnCanceledListener
{
    private static final String TAG = "UserAuthentication";

    public interface UserAuthListener {
        void WasAuthSuccessful(boolean successful, String message);
    }

    private Context m_Context;

    // Authentication
    private FirebaseAuth m_FirebaseAuth;
    private UserAuthListener m_UserAuthListener;

    // Database
    private FirebaseFirestore m_FirebaseDB;
    private User m_User;

    public FirebaseUserAuthentication(Context context)
    {
        m_Context = context;
        SetupFirebaseAuth();
    }

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    public void SetAuthListener(UserAuthListener listener) { m_UserAuthListener = listener; }

    public void RegisterNewUser(String username, String email, String password)
    {
        Log.d(TAG, "Creating user with email: '" + email + "' and password: '" + password + "'");
        m_User = new User();
        m_User.m_Email = email;
        m_User.m_Username = username;

        m_FirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            Log.d(TAG, "Create user task finished");
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully created user with email and password");
                FirebaseUser currentUser = m_FirebaseAuth.getCurrentUser();
                if (currentUser != null && m_User != null) {
                    m_User.m_UserID = currentUser.getUid();
                    TryAddNewUser();
                }
            }
            else {
                Log.d(TAG, "Couldn't create user with email and password");
                m_UserAuthListener.WasAuthSuccessful(false, "User authentication failed");
            }
        });
    }

    private void TryAddNewUser()
    {
        Log.d(TAG, "Trying to add new user...");
        DocumentReference userDoc = m_FirebaseDB.collection("users").document(m_User.m_UserID);
        userDoc.set(m_User.getMap())
                .addOnCompleteListener(this)
                .addOnSuccessListener(this)
                .addOnFailureListener(this)
                .addOnCanceledListener(this);
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
        m_FirebaseDB = FirebaseFirestore.getInstance();
    }

    @Override
    public void onComplete(@NonNull Task task)
    {
        Log.d(TAG, "Write user data to DB complete");
    }

    @Override
    public void onSuccess(Object o)
    {
        Log.d(TAG, "Successfully created user data");
        m_UserAuthListener.WasAuthSuccessful(true, "");
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {
        Log.d(TAG, "Write user data to DB failed");
        m_UserAuthListener.WasAuthSuccessful(false, "Couldn't create user data");
    }

    @Override
    public void onCanceled()
    {
        Log.d(TAG, "Write user data to DB canceled");
        m_UserAuthListener.WasAuthSuccessful(false, "Couldn't create user data");
    }
}
