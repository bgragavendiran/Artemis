package com.alliance.artemis.home.ui.dashboard;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alliance.artemis.R;
import com.alliance.artemis.adapters.GridImageAdapter;
import com.alliance.artemis.adapters.ImageAdapter;
import com.alliance.artemis.databinding.FragmentDashboardBinding;
import com.alliance.artemis.utils.ImageDataHelper;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardFragment extends Fragment {

    private RecyclerView imageRecyclerView;
    private ProgressBar syncProgressBar;
    private ImageDataHelper imageDataHelper;
    private GridImageAdapter adapter;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        syncProgressBar = root.findViewById(R.id.syncProgressBar);
        imageRecyclerView = root.findViewById(R.id.imageRecyclerView);

        // Initialize ImageDataHelper and fetch grouped image data
        imageDataHelper = new ImageDataHelper(requireContext());
        Map<String, Map<String, List<Map<String, Object>>>> groupedImages = imageDataHelper.getImagesGroupedByDate();

        // Flatten the grouped data into a list format
        List<Map<String, Object>> flatImageList = new ArrayList<>();
        for (Map<String, List<Map<String, Object>>> dateGroup : groupedImages.values()) {
            for (List<Map<String, Object>> folderGroup : dateGroup.values()) {
                flatImageList.addAll(folderGroup);
            }
        }
        int Total=imageDataHelper.getTotalImages();
        int uploaded=imageDataHelper.getUploadedImages();

        TextView textView0=root.findViewById(R.id.totalImage);
        TextView textView1=root.findViewById(R.id.outOfSync);
        TextView textView2=root.findViewById(R.id.inSync);
        textView0.setText(String.valueOf(Total));
        textView1.setText(String.valueOf(Total-uploaded));
        textView2.setText(String.valueOf(uploaded));

        // Initialize GridImageAdapter with the flattened list
        adapter = new GridImageAdapter(flatImageList, requireContext());

        // Calculate screen width and set dynamic span count
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int screenWidthPx = displayMetrics.widthPixels;

        // Convert item width from dp to px (120dp + 16dp padding)
        float itemWidthDp = 120 + 16;
        int itemWidthPx = (int) (itemWidthDp * displayMetrics.density);
        int spanCount = Math.max(1, screenWidthPx / itemWidthPx);  // Ensure at least 1 span

        // Initialize RecyclerView with calculated span count
        imageRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        imageRecyclerView.setAdapter(adapter);

        // Set progress bar based on sync percentage
        int syncProgress = (uploaded/Total)*100; // Assuming method exists
        syncProgressBar.setProgress(syncProgress);

        return root;
    }

}