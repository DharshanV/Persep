package com.perception.persep.firebase_helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.perception.persep.models.User;

public class FirebaseUserAuthentication implements ValueEventListener
{
    private static final String TAG = "UserAuthentication";

    public interface UserAuthListener {
        void WasAuthSuccessful(boolean successful, String message);
    }

    private Context m_Context;

    // Authentication
    private FirebaseAuth m_FirebaseAuth;
    private FirebaseAuth.AuthStateListener m_FirebaseAuthListener;
    private UserAuthListener m_UserAuthListener;

    // Database
    private FirebaseDatabase m_FirebaseDatabase;
    private DatabaseReference m_DatabaseRef;
    private User m_User;

    public FirebaseUserAuthentication(Context context)
    {
        m_Context = context;
        SetupFirebaseAuth();
    }

    public void onStart()
    {
        if (m_FirebaseAuthListener != null)
        {
            m_FirebaseAuth.addAuthStateListener(m_FirebaseAuthListener);
        }
    }

    public void onStop()
    {
        if (m_FirebaseAuthListener != null)
        {
            m_FirebaseAuth.removeAuthStateListener(m_FirebaseAuthListener);
        }
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
                m_UserAuthListener.WasAuthSuccessful(false, "Firebase couldn't create user with email and password");
            }
        });
    }

    private void TryAddNewUser()
    {
        Log.d(TAG, "Trying to add new user...");
//        m_UserAuthListener.WasAuthSuccessful(false, "");
        Query query = m_DatabaseRef.child("users").orderByChild("username").equalTo(m_User.m_Username);
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Log.d(TAG, "Query finished... on data changed");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                Log.d(TAG, "Query finished... on cancelled");
            }
        });
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
        m_FirebaseDatabase = FirebaseDatabase.getInstance();
        m_DatabaseRef = m_FirebaseDatabase.getReference();

        m_FirebaseAuthListener = firebaseAuth -> {
            Log.d(TAG, "Firebase auth state changed...");
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null)
            {
                Log.d(TAG, "User authenticated and signed in");
//                m_DatabaseRef.addListenerForSingleValueEvent(new ValueEventListener()
//                {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot)
//                    {
//                        TryAddNewUser();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error)
//                    {
//
//                    }
//                });
            }
            else
            {
                Log.d(TAG, "User authentication failed.");
                m_UserAuthListener.WasAuthSuccessful(false, "User authentication failed");
            }
        };
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot)
    {
        Log.d(TAG, "Query finished...");
        // Firebase user data has been created.
        // Now validate username
        for(DataSnapshot singleSnapshot : snapshot.getChildren()){
            if (singleSnapshot.exists())
            {
                // found a existing user with the name.
                Log.d(TAG, "Found an existing user with the same name");
                m_UserAuthListener.WasAuthSuccessful(false, "Username already exists");
                return;
            }
        }

        // username is valid, add userdata to database
        Log.d(TAG, "Writing out user data to database...");
        DatabaseReference database = m_FirebaseDatabase.getReference();
        database.child("users").child(m_User.m_UserID).setValue(m_User).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "Write to user database was sucessfull");
                m_UserAuthListener.WasAuthSuccessful(true, "");
            }
            else {
                Log.d(TAG, "Unable to write user data to database");
                m_UserAuthListener.WasAuthSuccessful(false, "Unable to write user data to database");
            }
        });
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error)
    {
        m_UserAuthListener.WasAuthSuccessful(false, "Unable to register user");
    }
}
