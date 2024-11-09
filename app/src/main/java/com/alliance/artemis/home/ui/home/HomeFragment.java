package com.alliance.artemis.home.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alliance.artemis.Camera2Activity;
import com.alliance.artemis.R;
import com.alliance.artemis.adapters.BatchAdapter;
import com.alliance.artemis.models.BatchItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;
import net.rehacktive.waspdb.WaspHash;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private BatchAdapter adapter;
    private List<BatchItem> batchList;
    private WaspDb waspDB;
    private WaspHash waspHash;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize an empty batchList to avoid null pointer issues
        batchList = new ArrayList<>();

        // Set up WaspDB in app's private directory
        File databaseDir = new File(requireContext().getFilesDir(), "waspdb");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        try {
            // Open or create the WaspDB database and hash map
            waspDB = WaspFactory.openOrCreateDatabase(databaseDir.getAbsolutePath(),"waspdb", "waspdb_password");
            waspHash = waspDB.openOrCreateHash("batchMap"); // Hash for storing BatchItem entries
        } catch (Exception e) {
            Log.e("HomeFragment", "Error initializing WaspDB", e);
        }

        // Initialize RecyclerView with batch data from WaspDB
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load data from WaspDB into batchList
        List<String> keys = waspHash.getAllKeys();
        if (keys != null) {
            if (!keys.isEmpty()){
            for (Object key : keys) {
                BatchItem item = (BatchItem) waspHash.get(key.toString());
                if (item != null) {
                    batchList.add(item);
                }
            }}
        }

        // Initialize RecyclerView
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Set up adapter with the retrieved data
        adapter = new BatchAdapter(batchList);
        recyclerView.setAdapter(adapter);

        // Initialize FAB to open Camera2Activity
        FloatingActionButton fab = root.findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            // Launch Camera2Activity to capture images
            Intent intent = new Intent(requireContext(), Camera2Activity.class);
            startActivity(intent);
        });

        return root;
    }
}
