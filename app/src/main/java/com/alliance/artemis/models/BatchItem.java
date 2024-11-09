package com.alliance.artemis.models;

import java.util.HashMap;

public class BatchItem {
    private String folderName;
    private String datetime;
    private int imageCount;

    private HashMap<String,String> hashMap;
    // No-argument constructor required by Kryo and WaspDB
    public BatchItem() {
        // Initialize fields if necessary, or leave them as default
    }

    public BatchItem(String folderName, String datetime, int imageCount,HashMap<String,String> hashMap) {
        this.folderName = folderName;
        this.datetime = datetime;
        this.imageCount = imageCount;
        this.hashMap=hashMap;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDatetime() {
        return datetime;
    }

    public int getImageCount() {
        return imageCount;
    }
    public HashMap<String,String> getHashMap(){
        return hashMap;
    }
}
