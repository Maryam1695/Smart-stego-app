package com.example.smartstego;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * This is the Login activity of the application.
 * It authenticates the user by checking the entered PIN against the saved encrypted PIN.
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Accessing local storage to check if the user has already set up a PIN
        SharedPreferences prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);
        String savedEncryptedOriginal = prefs.getString("original_pin", "");

        if (savedEncryptedOriginal.isEmpty()) {
            // If no PIN is set, redirect the user to the Setup screen first
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        // Displaying the login layout
        setContentView(R.layout.activity_login);

        // Binding XML components to Java variables
        TextInputEditText etLoginPass = findViewById(R.id.etLoginPass);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgotPassword);

        // Login button click listener
        btnLogin.setOnClickListener(v -> {
            String enteredPass = etLoginPass.getText().toString().trim();
            String savedEncryptedFake = prefs.getString("fake_pin", "");

            if (enteredPass.isEmpty()) {
                etLoginPass.setError("Password is required");
                return;
            }

            try {
                // Encrypt the entered password to compare it with the stored hash for security
                String encryptedEntered = CryptoStego.encrypt(enteredPass, "AppInternalSecurityKey");

                if (encryptedEntered.equals(savedEncryptedOriginal)) {
                    // Navigate to the main dashboard if the original PIN matches
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else if (encryptedEntered.equals(savedEncryptedFake)) {
                    // Navigate to a decoy (empty) screen if the fake PIN is entered
                    startActivity(new Intent(this, FakeActivity.class));
                    finish();
                } else {
                    // Show an error message if the PIN is incorrect
                    Toast.makeText(this, "Invalid Password!", Toast.LENGTH_SHORT).show();
                    etLoginPass.setText("");
                }
            } catch (Exception e) {
                Toast.makeText(this, "Security Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Redirect to Forgot Password activity
        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }
}
