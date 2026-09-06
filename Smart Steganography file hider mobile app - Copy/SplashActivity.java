package com.example.smartstego;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Yeh app ki pehli screen hai jo logo dikhati hai.
 * Ise "Splash Screen" kehte hain jo user ko welcome karti hai.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Humne yahan 2 second ka delay rakha hai taakay logo nazar aa sakay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 2 second baad automatically Login screen par bhej dega
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish(); // Ise band kar rahe hain taakay back dabane par ye wapis na aaye
        }, 2000);
    }
}