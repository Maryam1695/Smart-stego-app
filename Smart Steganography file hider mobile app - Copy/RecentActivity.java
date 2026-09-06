package com.example.smartstego;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

/**
 * This activity handles the "Recent Activity" log screen.
 * It is important for showing the examiner exactly what actions (embedding/extracting) 
 * have been performed recently within the app.
 */
public class RecentActivity extends AppCompatActivity {

    // UI variables for the list and empty state handling
    private RecyclerView rvRecent;
    private LinearLayout emptyView;
    private MaterialButton btnClearRecent;
    private RecentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        // Connecting XML views to Java variables
        rvRecent = findViewById(R.id.rvRecent);
        emptyView = findViewById(R.id.emptyRecentView);
        btnClearRecent = findViewById(R.id.btnClearRecent);

        // Setting a vertical layout manager for the RecyclerView
        rvRecent.setLayoutManager(new LinearLayoutManager(this));

        // Load saved activity logs from storage
        loadRecentActivity();

        // Logic for the button that clears all logs at once
        btnClearRecent.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Log")
                    .setMessage("Are you sure you want to delete all recent activity logs?")
                    .setPositiveButton("Yes, Clear", (dialog, which) -> {
                        // Deleting data using the Manager class
                        RecentManager.clearRecent(this);
                        loadRecentActivity(); // Refreshing the UI
                        Toast.makeText(this, "Activity log cleared successfully", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    // Function to fetch logs and update the list display
    private void loadRecentActivity() {
        List<RecentItem> recentList = RecentManager.getRecentItems(this);

        if (recentList.isEmpty()) {
            // If there's no activity, show the placeholder empty view
            rvRecent.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            btnClearRecent.setVisibility(View.GONE);
        } else {
            // If logs exist, display them in the RecyclerView list
            rvRecent.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            btnClearRecent.setVisibility(View.VISIBLE);

            // Attaching the adapter to the RecyclerView
            adapter = new RecentAdapter(recentList, this);
            rvRecent.setAdapter(adapter);
        }
    }
}
