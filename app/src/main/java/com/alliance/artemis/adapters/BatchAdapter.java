package com.alliance.artemis.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alliance.artemis.R;
import com.alliance.artemis.models.BatchItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {

    private final List<BatchItem> batchList;

    public BatchAdapter(List<BatchItem> batchList) {
        this.batchList = batchList != null ? batchList : new ArrayList<>(); // Ensure it's not null
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        BatchItem item = batchList.get(position);
        // Parse and format DateTimeUTC
        String dateTimeString = item.getHashMap().get("DateTimeUTC");
        String formattedDateTime = "";
        if (dateTimeString != null) {
            try {
                SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                Date date = originalFormat.parse(dateTimeString);
                SimpleDateFormat newFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US);
                formattedDateTime = newFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                formattedDateTime = dateTimeString; // Fallback to original if parsing fails
            }
        }

        holder.datetime.setText(formattedDateTime); // assuming batchDateUTC is defined
        holder.plantName.setText(item.getHashMap().get("PlantName"));
        holder.season.setText(item.getHashMap().get("Season"));
        holder.folderName.setText(item.getFolderName().trim().substring(0,7).toUpperCase());
        holder.imageCount.setText(String.valueOf(item.getImageCount()));
    }

    @Override
    public int getItemCount() {
        return batchList.size();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {
        TextView folderName,plantName,season, datetime, imageCount;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            plantName=itemView.findViewById(R.id.plantName);
            season=itemView.findViewById(R.id.season);
            folderName = itemView.findViewById(R.id.folderName);
            datetime = itemView.findViewById(R.id.datetime);
            imageCount = itemView.findViewById(R.id.imageCount);
        }
    }
}
