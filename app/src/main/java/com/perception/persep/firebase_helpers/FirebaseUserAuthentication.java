package com.perception.persep.firebase_helpers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseUserAuthentication
{
    private static final String TAG = "UserAuthentication";

    public interface UserAuthListener {
        void WasAuthSuccessful(boolean successful, String message);
    }

    private Context m_Context;
    private FirebaseAuth m_FirebaseAuth;
    private FirebaseAuth.AuthStateListener m_FirebaseAuthListener;
    private UserAuthListener m_UserAuthListener;

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
        m_UserAuthListener.WasAuthSuccessful(false, "Code not implemented");
    }

    private void SetupFirebaseAuth()
    {
        Log.d(TAG, "Setting up Firebase auth.");
        m_FirebaseAuth = FirebaseAuth.getInstance();
//        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
//        DatabaseReference databaseRef = firebaseDatabase.getReference();
//
//        m_FirebaseAuthListener = firebaseAuth -> {
//            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//            if (currentUser != null)
//            {
//                Log.d(TAG, "User signed in.");
//                databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        // Firebase user data has been created.
//                        // Now validate username
//                        for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
//                            if (singleSnapshot.exists())
//                            {
//                                // found a existing user with the name.
//                                m_UserAuthListener.WasAuthSuccessful(false, "Username already exists");
//                                return;
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        m_UserAuthListener.WasAuthSuccessful(false, "Unable to register user");
//                    }
//                });
//            }
//            else
//            {
//                Log.d(TAG, "User signed out.");
//                m_UserAuthListener.WasAuthSuccessful(false, "User signed out");
//            }
//        };
    }
}
