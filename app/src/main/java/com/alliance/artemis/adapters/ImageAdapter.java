package com.alliance.artemis.adapters;

// ImageAdapter.java

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alliance.artemis.R;

import java.util.List;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final Map<String, Map<String, List<Map<String, Object>>>> groupedImages;

    public ImageAdapter(Map<String, Map<String, List<Map<String, Object>>>> groupedImages) {
        this.groupedImages = groupedImages;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Use position to iterate through groupedImages
        String date = (String) groupedImages.keySet().toArray()[position];
        holder.titleTextView.setText(date);

        // Bind additional grouped data here based on folder and images
        Map<String, List<Map<String, Object>>> folders = groupedImages.get(date);
        holder.subtitleTextView.setText("Folders: " + folders.keySet().size()); // Example
    }

    @Override
    public int getItemCount() {
        return groupedImages.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, subtitleTextView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleText);
            subtitleTextView = itemView.findViewById(R.id.subtitleText);
        }
    }
}
