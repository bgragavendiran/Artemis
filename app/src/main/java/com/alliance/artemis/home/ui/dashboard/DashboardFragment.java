package com.alliance.artemis.home.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alliance.artemis.R;
import com.alliance.artemis.adapters.GridImageAdapter;
import com.alliance.artemis.adapters.ImageAdapter;
import com.alliance.artemis.databinding.FragmentDashboardBinding;
import com.alliance.artemis.utils.ImageDataHelper;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import androidx.core.app.NotificationCompat;
public class DashboardFragment extends Fragment {

    private RecyclerView imageRecyclerView;
    private ProgressBar syncProgressBar;
    private ImageDataHelper imageDataHelper;
    private GridImageAdapter adapter;
    private MaterialButton sync_button;
    private AmazonS3Client s3Client;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        createNotificationChannel();
        syncProgressBar = root.findViewById(R.id.syncProgressBar);
        imageRecyclerView = root.findViewById(R.id.imageRecyclerView);
        sync_button = root.findViewById(R.id.sync_button);

        imageDataHelper = new ImageDataHelper(requireContext());
        Map<String, Map<String, List<Map<String, Object>>>> groupedImages = imageDataHelper.getImagesGroupedByDate();

        List<Map<String, Object>> flatImageList = new ArrayList<>();
        for (Map<String, List<Map<String, Object>>> dateGroup : groupedImages.values()) {
            for (List<Map<String, Object>> folderGroup : dateGroup.values()) {
                flatImageList.addAll(folderGroup);
            }
        }

        int totalImages = imageDataHelper.getTotalImages();
        int uploadedImages = imageDataHelper.getUploadedImages();
        TextView totalImageText = root.findViewById(R.id.totalImage);
        TextView outOfSyncText = root.findViewById(R.id.outOfSync);
        TextView inSyncText = root.findViewById(R.id.inSync);
        totalImageText.setText(String.valueOf(totalImages));
        outOfSyncText.setText(String.valueOf(totalImages - uploadedImages));
        inSyncText.setText(String.valueOf(uploadedImages));
        if (totalImages!=0){
            syncProgressBar.setProgress((uploadedImages * 100) / totalImages);
        } else {
            syncProgressBar.setIndeterminate(false);
            syncProgressBar.setProgress(0);
        }

        adapter = new GridImageAdapter(flatImageList, requireContext());
        imageRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), calculateSpanCount()));
        imageRecyclerView.setAdapter(adapter);

        fetchAWSCredentials();

        sync_button.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                syncData();
            } else {
                showLoginDialog();
            }
        });

        return root;
    }

    private int calculateSpanCount() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int screenWidthPx = displayMetrics.widthPixels;
        int itemWidthPx = (int) (136 * displayMetrics.density); // 120dp item + 16dp padding
        return Math.max(1, screenWidthPx / itemWidthPx);
    }

    private boolean isUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null;
    }

    private void showLoginDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View loginView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        EditText emailInput = loginView.findViewById(R.id.emailInput);
        EditText passwordInput = loginView.findViewById(R.id.passwordInput);
        builder.setView(loginView)
                .setTitle("Sign In")
                .setPositiveButton("Login", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    String password = passwordInput.getText().toString().trim();
                    signInUser(email, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signInUser(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        syncData();
                    }
                });
    }

    private void fetchAWSCredentials() {
        DatabaseReference credentialsRef = FirebaseDatabase.getInstance().getReference("aws_credentials");
        credentialsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String accessKey = snapshot.child("accessKey").getValue(String.class);
                    String secretKey = snapshot.child("secretKey").getValue(String.class);
                    configureAndStartAWSS3Client(accessKey, secretKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DashboardFragment", "Error fetching AWS credentials", error.toException());
            }
        });
    }

    private void configureAndStartAWSS3Client(String accessKey, String secretKey) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = new AmazonS3Client(credentials);
        syncData();
    }

    private void syncData() {
        List<Map<String, Object>> unsyncedImages = imageDataHelper.getImagesNotSynced();
        if (!unsyncedImages.isEmpty()) {
            new S3UploadTask(unsyncedImages).execute();
        }
    }

    private void saveImageUrlToRTDB(Map<String, Object> imageData, String imageUrl) {
        String plantName = (String) imageData.get("PlantName");
        String date = (String) imageData.get("Date");
        String folderName = (String) imageData.get("FolderName");
        String imageName = (String) imageData.get("ImageName");

        // Sanitize image name to remove unsupported characters
        String sanitizedImageName = imageName.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_");

        // Build the structured Firebase Database reference
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("images")
                .child(plantName)
                .child(date)
                .child(folderName)
                .child(sanitizedImageName);

        // Store image data under the structured path
        dbRef.child("url").setValue(imageUrl);
        dbRef.child("status").setValue("uploaded");
        dbRef.child("plantName").setValue(plantName);
        dbRef.child("date").setValue(date);
        dbRef.child("folderName").setValue(folderName);
        imageDataHelper.updateImageSyncStatus(imageName, true); // Update WaspDB
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        s3Client = null;
    }
    private static final String CHANNEL_ID = "upload_channel";

    private void createNotificationChannel() {
        CharSequence name = "Upload Notifications";
        String description = "Shows progress of image uploads to S3";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }




    private NotificationManager notificationManager;
    private static final int UPLOAD_NOTIFICATION_ID = 1;


    private class S3UploadTask extends AsyncTask<Void, Integer, Void> {
        private int totalImages;
        private List<Map<String, Object>> imagesToUpload;

        public S3UploadTask(List<Map<String, Object>> imagesToUpload) {
            this.imagesToUpload = imagesToUpload;
            this.totalImages = imagesToUpload.size();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            showUploadNotification(0, totalImages);
            syncProgressBar.setIndeterminate(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i < imagesToUpload.size(); i++) {
                uploadImageToS3(imagesToUpload.get(i));
                publishProgress(i + 1);  // Update progress
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            showUploadNotification(values[0], totalImages);

            syncProgressBar.setProgress((values[0] * 100) / totalImages);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            showUploadNotification(totalImages, totalImages);
        }
        private void uploadImageToS3(Map<String, Object> image) {
            if (s3Client == null) {
                Log.e("DashboardFragment", "S3 Client not initialized");
                return;
            }

            String bucketName = "afd-public-bucket";
            String imageName = (String) image.get("ImageName");
            String localPath = (String) image.get("localPath");

            if (localPath == null || localPath.isEmpty()) {
                Log.e("DashboardFragment", "Local path is empty or null for image: " + imageName);
                return;
            }

            File file = new File(localPath);
            if (file.exists()) {
                try {
                    s3Client.putObject(bucketName, imageName, file);
                    String imageUrl = s3Client.getUrl(bucketName, imageName).toString();
                    saveImageUrlToRTDB(image, imageUrl);
                    imageDataHelper.updateImageSyncStatus(imageName, true);
                } catch (Exception e) {
                    Log.e("DashboardFragment", "Failed to upload image to S3", e);
                }
            } else {
                Log.e("DashboardFragment", "File does not exist at path: " + localPath);
            }
        }
    }

    private void showUploadNotification(int current, int total) {
        String title = "Uploading images";
        String content = "Uploading " + current + " of " + total;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_cloud_sync_24)  // Replace with your app's icon
                .setContentTitle(title)
                .setContentText(content)
                .setProgress(total, current, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (current == total) {
            builder.setContentText("Upload complete")
                    .setProgress(0, 0, false)
                    .setOngoing(false);
        }

        notificationManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
    }

}
