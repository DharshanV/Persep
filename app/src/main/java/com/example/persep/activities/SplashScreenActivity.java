package com.example.persep.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.persep.R;

import java.util.ArrayList;
import java.util.List;

public class SplashScreenActivity extends Activity
{
    private static final String TAG = "SplashScreen";

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private static final int HOME_ACTIVITY_DELAY_MS = 2000;
    private static final int FINISH_ACTIVITY_DELAY_MS = 1500;
    private String[] m_ManifestPermissions = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        m_ManifestPermissions = GetAllManifestPermission(this).clone();
        if(CheckAndRequestRequiredPermissions())
        {
            StartHomeActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(requestCode != REQUEST_ID_MULTIPLE_PERMISSIONS)
        {
            return;
        }

        if(grantResults.length > 0)
        {
            boolean allPermissionGranted = true;
            for(int grantResult : grantResults)
            {
                if(grantResult != PackageManager.PERMISSION_GRANTED)
                {
                    allPermissionGranted = false;
                    break;
                }
            }

            if(allPermissionGranted)
            {
                StartHomeActivity();
            }
            else
            {
                Log.d(TAG, "Some permission are not granted. Asking again...");

                boolean shouldRequestPermissionRationale = false;
                for(String permission : m_ManifestPermissions)
                {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                    {
                        shouldRequestPermissionRationale = true;
                        break;
                    }
                }

                if(shouldRequestPermissionRationale)
                {
                    ShowDialogOkCancel("Some permission are still required to continue",
                        (dialog, which) ->
                        {
                            if(which == DialogInterface.BUTTON_POSITIVE)
                            {
                                if(CheckAndRequestRequiredPermissions())
                                {
                                    StartHomeActivity();
                                }
                            }
                            else if(which == DialogInterface.BUTTON_NEGATIVE)
                            {
                                Log.d(TAG, "User denied re-permission. Quitting app.");
                                StopSplashScreenActivity();
                            }
                        });
                }
                else
                {
                    StopSplashScreenActivity();
                }
            }
        }
    }

    private void StartHomeActivity()
    {
        Log.d(TAG, "All permission granted. Starting home activity.");
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
            finish();
        }, HOME_ACTIVITY_DELAY_MS);
    }

    private void StopSplashScreenActivity()
    {
        Toast.makeText(this, "Go to settings and enable required permissions", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(this::finish, FINISH_ACTIVITY_DELAY_MS);
    }

    private boolean CheckAndRequestRequiredPermissions()
    {
        List<String> permissionNeeded = new ArrayList<>();

        for(String permission : m_ManifestPermissions)
        {
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            {
                permissionNeeded.add(permission);
            }
        }

        if(!permissionNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, permissionNeeded.toArray(new String[permissionNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }

        return true;
    }

    public static String[] GetAllManifestPermission(Context context)
    {
        try
        {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) { throw new RuntimeException ("Unable to get manifest permission", e); }
    }

    private void ShowDialogOkCancel(String message, DialogInterface.OnClickListener okListener)
    {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }
}