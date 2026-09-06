package com.example.smartstego;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

/**
 * This activity displays the list of all hidden files.
 * It includes logic for permission handling and an option to clear the history log 
 * so users can manage their records effectively.
 */
public class HistoryActivity extends AppCompatActivity {

    // UI elements to display the history list and empty state
    private RecyclerView rvHistory;
    private LinearLayout emptyView;
    private MaterialButton btnClearHistory;
    private HistoryAdapter adapter;

    // Launcher to request storage permissions from the user
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // Load history once permissions are granted
                syncAndLoadHistory();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Binding XML widgets to Java code
        rvHistory = findViewById(R.id.rvHistory);
        emptyView = findViewById(R.id.emptyView);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        // Setting a vertical layout manager for the list
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Checking permissions before loading data
        checkPermissionsAndLoad();

        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> {
                // Confirmation dialog before deleting all history records
                new AlertDialog.Builder(this)
                        .setTitle("Clear History")
                        .setMessage("Are you sure you want to delete all history records? (Note: Original files remain safe on your phone)")
                        .setPositiveButton("Yes, Clear", (dialog, which) -> {
                            HistoryManager.clearHistory(this); // Delete via manager
                            syncAndLoadHistory(); // Refresh the screen
                            Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    // Handles permission checks for different Android versions (Android 13+ vs Older)
    private void checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires separate Image and Audio permissions
            String[] perms = {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO};
            if (ContextCompat.checkSelfPermission(this, perms[0]) == PackageManager.PERMISSION_GRANTED) {
                syncAndLoadHistory();
            } else {
                permissionLauncher.launch(perms);
            }
        } else {
            // Standard storage permission for older Android versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                syncAndLoadHistory();
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    // Fetches history data and updates the UI accordingly
    private void syncAndLoadHistory() {
        List<HistoryItem> historyList = HistoryManager.getHistory(this);

        if (historyList == null || historyList.isEmpty()) {
            // Show the "No Records" view if the list is empty
            rvHistory.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
        } else {
            // Show the RecyclerView if data exists
            rvHistory.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            if (btnClearHistory != null) btnClearHistory.setVisibility(View.VISIBLE);

            // Setting up the adapter to populate the list cards
            adapter = new HistoryAdapter(historyList, this);
            rvHistory.setAdapter(adapter);
        }
    }
}
