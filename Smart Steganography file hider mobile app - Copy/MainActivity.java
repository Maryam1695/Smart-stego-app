package com.example.smartstego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

/**
 * This is the main dashboard of the application where all features are accessed.
 * We used a card-based layout to make it look modern and professional.
 */
public class MainActivity extends AppCompatActivity {

    // Declaring UI variables for the dashboard cards and settings button
    ImageButton btnSettings;
    MaterialCardView btnImageStego, btnAudioStego, btnExtract, btnHistory, btnRecent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connecting XML views to Java variables
        btnSettings = findViewById(R.id.btnSettings);
        btnImageStego = findViewById(R.id.btnImageStego);
        btnAudioStego = findViewById(R.id.btnAudioStego);
        btnExtract = findViewById(R.id.btnExtractData);
        btnHistory = findViewById(R.id.btnMyFiles);
        btnRecent = findViewById(R.id.btnRecentActivity);

        // Navigation to Image Steganography screen
        btnImageStego.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ImageHideActivity.class));
        });

        // Navigation to Audio Steganography screen
        btnAudioStego.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AudioHideActivity.class));
        });

        // Navigation to the Data Extraction screen
        btnExtract.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ExtractActivity.class));
        });

        // Navigation to view the history of encoded files
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        // Navigation to the Recent Activity Log
        btnRecent.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecentActivity.class));
        });

        // Settings popup menu logic for changing security credentials
        btnSettings.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, btnSettings);
            popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_change_password) {
                    // Open the activity to update passwords and security questions
                    startActivity(new Intent(MainActivity.this, ChangePasswordActivity.class));
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }
}
