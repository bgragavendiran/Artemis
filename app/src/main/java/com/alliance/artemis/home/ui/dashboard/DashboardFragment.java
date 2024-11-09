package com.alliance.artemis.home.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private RecyclerView imageRecyclerView;
    private ProgressBar syncProgressBar;
    private TextView titleText, subtitleText;
    private ImageDataHelper imageDataHelper;
    private GridImageAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        titleText = root.findViewById(R.id.titleText);
        subtitleText = root.findViewById(R.id.subtitleText);
        syncProgressBar = root.findViewById(R.id.syncProgressBar);
        imageRecyclerView = root.findViewById(R.id.imageRecyclerView);

        titleText.setText("Image Dashboard");
        subtitleText.setText("Manage your synced images");

        // Initialize ImageDataHelper and fetch grouped image data
        imageDataHelper = new ImageDataHelper();
        Map<String, Map<String, List<Map<String, Object>>>> groupedImages = imageDataHelper.getImagesGroupedByDate();

        // Initialize RecyclerView and adapter
        imageRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        ImageAdapter imageAdapter = new ImageAdapter(groupedImages);
        imageRecyclerView.setAdapter(imageAdapter);

        // Set progress bar based on sync percentage
        int syncProgress = imageDataHelper.getSyncPercentage(); // Assuming method exists
        syncProgressBar.setProgress(syncProgress);
        return root;
    }
}