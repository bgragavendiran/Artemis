package com.alliance.artemis.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alliance.artemis.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.ViewHolder> {
    private List<Map<String, Object>> flattenedData;

    public GridImageAdapter(Map<String, Map<String, List<Map<String, Object>>>> groupedData) {
        this.flattenedData = flattenGroupedData(groupedData);
    }

    private List<Map<String, Object>> flattenGroupedData(Map<String, Map<String, List<Map<String, Object>>>> groupedData) {
        List<Map<String, Object>> flattenedList = new ArrayList<>();
        for (Map<String, List<Map<String, Object>>> dateGroup : groupedData.values()) {
            for (List<Map<String, Object>> images : dateGroup.values()) {
                flattenedList.addAll(images);
            }
        }
        return flattenedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> imageData = flattenedData.get(position);

        // Retrieve data from imageData
        String imageName = (String) imageData.get("ImageName");
        String plantName = (String) imageData.get("PlantName");
        String date = (String) imageData.get("Date");
        boolean isUploaded = (boolean) imageData.get("Uploaded");

        // Set image preview using Glide or any other image loading library
        String imagePath = (String) imageData.get("ImagePath");
        Glide.with(holder.itemView.getContext())
                .load(imagePath)
                .placeholder(R.drawable.imageedit_6_6771742125)
                .into(holder.imageView);

        // Set the date, title, and subtitle
        holder.titleTextView.setText(imageName);
        holder.subtitleTextView.setText(plantName + " | " + date);

        // Show sync status icon if uploaded
        holder.syncStatusIcon.setVisibility(isUploaded ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return flattenedData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView, subtitleTextView;
        ImageView syncStatusIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_preview);
            titleTextView = itemView.findViewById(R.id.image_title);
            subtitleTextView = itemView.findViewById(R.id.plant_name);
            syncStatusIcon = itemView.findViewById(R.id.sync_status_icon);
        }
    }
}
