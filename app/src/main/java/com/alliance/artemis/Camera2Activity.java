package com.alliance.artemis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alliance.artemis.home.HomeActivity;
import com.alliance.artemis.models.BatchItem;
import com.alliance.artemis.utils.HashUtil;
import com.google.android.material.textfield.TextInputEditText;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;
import net.rehacktive.waspdb.WaspHash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Camera2Activity extends AppCompatActivity {
    private static final int DELAY_BETWEEN_CAPTURES_MS = 1000; // 1 second delay (adjust as needed)

    private Handler captureHandler;
    private Runnable captureRunnable;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Size previewSize;
    private ImageReader imageReader;
    private boolean capturing = false;
    private ImageButton startStopButton;
    private TextView imageCountText;
    private int imageCount = 0;

    private String batchID;
    private String batchDateUTC;
    private File batchFolder;

    private WaspDb waspDB;
    private WaspHash waspHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        textureView = findViewById(R.id.previewFrame);
        imageCountText = findViewById(R.id.imageCountText);
        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(view -> toggleImageCapture());

        // Initialize WaspDB
        File databaseDir = new File(getFilesDir(), "waspdb");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }
        try {
            waspDB = WaspFactory.openOrCreateDatabase(databaseDir.getAbsolutePath(),"waspdb", "waspdb_password");
            waspHash = waspDB.openOrCreateHash("batchMap"); // HashMap for storing batch data
        } catch (Exception e) {
            e.printStackTrace();
        }

        textureView.setSurfaceTextureListener(textureListener);
        // Initialize handler for timed capture
        captureHandler = new Handler();
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // Get the best possible preview size
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // Find the largest square resolution (1:1) supported by the camera
            Size largestSquareSize = findLargestSquareSize();

            // Initialize ImageReader with 1:1 aspect ratio only for capturing images (not for preview)
            imageReader = ImageReader.newInstance(largestSquareSize.getWidth(), largestSquareSize.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                saveImage(image);
            }, backgroundHandler);

            // Set up preview surface
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);

            // Configure the capture session with only the preview surface
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);  // Only add preview surface

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(Camera2Activity.this, "Failed to configure camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startImageCapture() {
        capturing = true;
        startStopButton.setImageResource(R.drawable.stop_button);
        imageCount = 0;

        // Generate unique batch ID and create folder
        batchDateUTC = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        batchID = HashUtil.getSHA256Hash(batchDateUTC);

        // Set up the folder within the app’s internal storage
        batchFolder = new File(getFilesDir(), batchID);
        if (!batchFolder.exists()) {
            if (!batchFolder.mkdirs()) {
                Log.e("Camera2Activity", "Failed to create directory: " + batchFolder.getAbsolutePath());
                return;
            }
        }
        imageCountText.setText("Images Captured: " + imageCount);
        Log.d("TAG", "startImageCapture: Starting image capture");

        // Start capturing images with a timed delay
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (capturing) {
                    captureImage(); // Capture image at each interval
                    captureHandler.postDelayed(this, DELAY_BETWEEN_CAPTURES_MS); // Delay for next capture
                }
            }
        };
        captureHandler.post(captureRunnable); // Start first capture immediately
    }

    private void captureImage() {
        if (cameraDevice == null || captureSession == null) return;

        try {
            // Use a separate CaptureRequest for still image capture
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Set the capture listener to handle the completion of the capture
            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d("Camera2Activity", "Image captured at interval");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // Method to find the largest 1:1 (square) resolution
    private Size findLargestSquareSize() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // Filter sizes to find the largest 1:1 resolution
            Size largestSquareSize = null;
            for (Size size : map.getOutputSizes(ImageFormat.JPEG)) {
                if (size.getWidth() == size.getHeight()) { // Check for 1:1 aspect ratio
                    if (largestSquareSize == null || (size.getWidth() > largestSquareSize.getWidth())) {
                        largestSquareSize = size;
                    }
                }
            }
            return largestSquareSize != null ? largestSquareSize : new Size(1024, 1024); // Default to 1024x1024 if no square found
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return new Size(1024, 1024); // Fallback size if there's an issue
    }

    private void updatePreview() {
        if (cameraDevice == null) return;

        previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        try {
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void toggleImageCapture() {
        if (capturing) {
            stopImageCapture();
        } else {
            startImageCapture();
        }
    }
  private void saveImage(Image image) {
    if (image == null) {
        Log.e("Camera2Activity", "Image is null, skipping save.");
        return;
    }

    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    image.close();

    // Generate file name for the image
    File imageFile = new File(batchFolder, batchID + "_IMG_" + imageCount + ".jpg");

    try (FileOutputStream output = new FileOutputStream(imageFile)) {
        output.write(bytes);
        imageCount++;

        Log.d("Camera2Activity", "Image saved to: " + imageFile.getAbsolutePath() + ", count: " + imageCount);

        runOnUiThread(() -> imageCountText.setText("Images Captured: " + imageCount));
    } catch (IOException e) {
        Log.e("Camera2Activity", "Failed to save image: " + e.getMessage());
        e.printStackTrace();
    }
}

    private void stopImageCapture() {
        capturing = false;
        startStopButton.setImageResource(R.drawable.baseline_camera_alt_24);
        Log.d("TAG", "stopImageCapture: "+imageCount);
        imageCountText.setText("Total Images Captured: " + imageCount);
        stopBackgroundThread();
        // Stop the timed capture
        if (captureHandler != null && captureRunnable != null) {
            captureHandler.removeCallbacks(captureRunnable);
        }
        showCaptureFormDialog();
        Toast.makeText(this, "Capture stopped. Images saved to: " + batchFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }



    // Method to show a dialog form for collecting additional information
    private void showCaptureFormDialog() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Capture Details");

        // Inflate custom layout
        View formView = getLayoutInflater().inflate(R.layout.dialog_capture_form, null);
        builder.setView(formView);

        // Initialize EditText and Spinners
        TextInputEditText plantNameEditText = formView.findViewById(R.id.plantNameEditText);
        Spinner seasonSpinner = formView.findViewById(R.id.seasonSpinner);
        Spinner climateSpinner = formView.findViewById(R.id.climateSpinner);

        // Populate Spinners
        ArrayAdapter<CharSequence> seasonAdapter = ArrayAdapter.createFromResource(this,
                R.array.season_array, android.R.layout.simple_spinner_item);
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(seasonAdapter);

        ArrayAdapter<CharSequence> climateAdapter = ArrayAdapter.createFromResource(this,
                R.array.climate_array, android.R.layout.simple_spinner_item);
        climateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        climateSpinner.setAdapter(climateAdapter);

        // Set up dialog buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String plantName = plantNameEditText.getText().toString();
            String season = seasonSpinner.getSelectedItem().toString();
            String climate = climateSpinner.getSelectedItem().toString();

            // Collect data into hashMap
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("DateTimeUTC", batchDateUTC); // assuming batchDateUTC is defined
            hashMap.put("PlantName", plantName);
            hashMap.put("Season", season);
            hashMap.put("Climate", climate);

            // Save batch metadata to WaspDB
            BatchItem batchItem = new BatchItem(batchID, batchFolder.getAbsolutePath(), imageCount, hashMap);
            waspHash.put(batchID, batchItem);

            Toast.makeText(this, "Capture details saved!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Camera2Activity.this, HomeActivity.class));
            finish();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
