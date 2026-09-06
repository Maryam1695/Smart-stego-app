package com.example.smartstego;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.OnBackPressedCallback;

/**
 * This "Fake Activity" is a decoy feature of the app.
 * If someone forces the user to open the app, entering the Fake PIN will open this screen 
 * to show an empty gallery, keeping the real data safe.
 */
public class FakeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake);

        // Setting the status bar color to match the decoy gallery theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#1565C0"));
        }

        // Setting up the toolbar with the title "Gallery" to look realistic
        Toolbar toolbar = findViewById(R.id.fakeToolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Gallery");
            }
        }

        // Close the entire app when back is pressed for extra security
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }
}
