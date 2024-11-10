package com.alliance.artemis.utils;

import android.content.Context;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;
import net.rehacktive.waspdb.WaspHash;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageDataHelper {
    private WaspDb waspDB;
    private WaspHash imageHash;

    Context context;
    public ImageDataHelper(Context context) {
        this.context=context;
        File databaseDir = new File(context.getFilesDir(), "waspdb");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }
        try {
            this.waspDB = WaspFactory.openOrCreateDatabase(databaseDir.getAbsolutePath(), "waspdb", "waspdb_password");
            this.imageHash = waspDB.openOrCreateHash("imageTracking");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addImageEntry(String date, String folderName, String plantName, String imageName, boolean uploaded) {
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("Date", date);
        imageData.put("FolderName", folderName);
        imageData.put("PlantName", plantName);
        imageData.put("ImageName", imageName);
        imageData.put("Uploaded", uploaded);

        imageHash.put(imageName, imageData);
    }

    public Map<String, Map<String, List<Map<String, Object>>>> getImagesGroupedByDate() {
        Map<String, Map<String, List<Map<String, Object>>>> groupedData = new HashMap<>();

        for (Map.Entry<Object, Object> entry : imageHash.getAllData().entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }

            Map<String, Object> imageData = (Map<String, Object>) entry.getValue();
            String date = imageData.get("Date") != null ? (String) imageData.get("Date") : "Unknown Date";
            String folderName = imageData.get("FolderName") != null ? (String) imageData.get("FolderName") : "Unknown Folder";

            if (entry.getKey() != null) {
                imageData.put("ImagePath", entry.getKey());
            }

            groupedData
                    .computeIfAbsent(date, k -> new HashMap<>())
                    .computeIfAbsent(folderName, k -> new ArrayList<>())
                    .add(imageData);
        }
        return groupedData;
    }

    public int getSyncPercentage() {
        int totalImages = getTotalImages();
        int uploadedImages = getUploadedImages();

        return totalImages == 0 ? 0 : (uploadedImages * 100) / totalImages;
    }

    public void updateImageSyncStatus(String imageName, boolean uploaded) {
        Map<String, Object> imageData = (Map<String, Object>) imageHash.get(imageName);
        if (imageData != null) {
            imageData.put("Uploaded", uploaded);
            imageHash.put(imageName, imageData);
        }
    }

    // Method to get the total number of images
    public int getTotalImages() {
        return imageHash.getAllData().size();
    }

    // Method to get the total number of uploaded images
    public int getUploadedImages() {
        int uploadedImages = 0;
        for (Map.Entry<Object, Object> entry : imageHash.getAllData().entrySet()) {
            Map<String, Object> imageData = (Map<String, Object>) entry.getValue();
            if ((boolean) imageData.getOrDefault("Uploaded", false)) {
                uploadedImages++;
            }
        }
        return uploadedImages;
    }

    public List<Map<String, Object>> getImagesNotSynced() {
        List<Map<String, Object>> unsyncedImages = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : imageHash.getAllData().entrySet()) {
            Map<String, Object> imageData = (Map<String, Object>) entry.getValue();
            boolean isUploaded = (boolean) imageData.getOrDefault("Uploaded", false);

            if (!isUploaded) {
                String imageName = (String) imageData.get("ImageName");
                String folderName = (String) imageData.get("FolderName");

                // Create full path by combining folder name and image path
                File filePath = new File(context.getFilesDir(), folderName);
                String fullLocalPath = new File(filePath, imageName).getAbsolutePath();
                imageData.put("localPath", fullLocalPath);

                // Include all necessary metadata, including the key as "ImagePath"
                imageData.put("ImagePath", entry.getKey());
                unsyncedImages.add(imageData);
            }
        }
        return unsyncedImages;
    }

}
