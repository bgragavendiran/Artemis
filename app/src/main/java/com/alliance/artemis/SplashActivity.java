package com.alliance.artemis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alliance.artemis.home.HomeActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 1000; // Duration for splash screen in milliseconds
    private static final int PERMISSION_REQUEST_CODE = 100; // Request code for permission requests

    // List of required permissions
    private final String[] requiredPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Set the splash screen layout

        // Check and request permissions
        if (hasAllPermissions()) {
            // If permissions are already granted, start the splash timer
            startSplashTimer();
        } else {
            // Request permissions if they are not granted
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    // Check if all required permissions are granted
    private boolean hasAllPermissions() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Start the splash timer to transition to HomeActivity
    private void startSplashTimer() {
        new Handler().postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(mainIntent);
            finish(); // Close the splash screen so it doesn't remain in the back stack
        }, SPLASH_DISPLAY_LENGTH);
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Start the splash timer after permissions are granted
                startSplashTimer();
            } else {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show();
                startSplashTimer();
            }
        }
    }
}
