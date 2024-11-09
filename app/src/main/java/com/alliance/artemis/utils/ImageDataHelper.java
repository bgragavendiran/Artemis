package com.alliance.artemis.utils;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspHash;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageDataHelper {
    private WaspDb waspDB;
    private WaspHash imageHash;

    public ImageDataHelper() {
        this.waspDB = waspDB;
        this.imageHash = waspDB.openOrCreateHash("imageTracking");
    }

    // Method to add an image entry to WaspHash
    public void addImageEntry(String date, String folderName, String plantName, String imageName, boolean uploaded) {
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("Date", date);
        imageData.put("FolderName", folderName);
        imageData.put("PlantName", plantName);
        imageData.put("ImageName", imageName);
        imageData.put("Uploaded", uploaded);

        // Save entry with a unique key per image
        imageHash.put(imageName, imageData);
    }

    // Method to get grouped data by date
    public Map<String, Map<String, List<Map<String, Object>>>> getImagesGroupedByDate() {
    Map<String, Map<String, List<Map<String, Object>>>> groupedData = new HashMap<>();

    // Loop through all entries in the image hash table
    for (Map.Entry<Object, Object> entry : imageHash.getAllData().entrySet()) {
        // Ensure that the value is a valid Map object
        if (!(entry.getValue() instanceof Map)) {
            continue; // Skip non-map entries
        }

        Map<String, Object> imageData = (Map<String, Object>) entry.getValue();

        // Safely retrieve the date and folder name with null checks
        String date = imageData.get("Date") != null ? (String) imageData.get("Date") : "Unknown Date";
        String folderName = imageData.get("FolderName") != null ? (String) imageData.get("FolderName") : "Unknown Folder";

        // Add the image path if the key exists
        if (entry.getKey() != null) {
            imageData.put("ImagePath", entry.getKey());
        }

        // Use computeIfAbsent to structure the nested data structure
        groupedData
                .computeIfAbsent(date, k -> new HashMap<>())
                .computeIfAbsent(folderName, k -> new ArrayList<>())
                .add(imageData);
    }
    return groupedData;
}


    // Method to calculate the overall sync percentage
    public int getSyncPercentage() {
        int totalImages = 0;
        int uploadedImages = 0;

        for (Map.Entry<Object, Object> entry : imageHash.getAllData().entrySet()) {
            Map<String, Object> imageData = (Map<String, Object>) entry.getValue();
            boolean isUploaded = (boolean) imageData.get("Uploaded");

            totalImages++;
            if (isUploaded) {
                uploadedImages++;
            }
        }

        return totalImages == 0 ? 0 : (uploadedImages * 100) / totalImages;
    }

    // Method to update sync status of an image
    public void updateImageSyncStatus(String imageName, boolean uploaded) {
        Map<String, Object> imageData = (Map<String, Object>) imageHash.get(imageName);
        if (imageData != null) {
            imageData.put("Uploaded", uploaded);
            imageHash.put(imageName, imageData); // Save the updated status
        }
    }
}
