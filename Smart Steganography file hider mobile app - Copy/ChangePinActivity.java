package com.example.smartstego;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

/**
 * This activity is used to change the user's PIN.
 * It verifies the current PIN before allowing the user to save a new one.
 */
public class ChangePinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using the existing change password layout
        setContentView(R.layout.activity_change_password);

        // Binding UI elements to Java objects
        TextInputEditText etOldPass = findViewById(R.id.etOldPass);
        TextInputEditText etNewOriginal = findViewById(R.id.etNewOriginalPass);
        TextInputEditText etNewFake = findViewById(R.id.etNewFakePass);
        ProgressBar barOriginal = findViewById(R.id.barNewOriginal);
        ProgressBar barFake = findViewById(R.id.barNewFake);
        TextView tvOriginal = findViewById(R.id.tvStrengthOriginal);
        TextView tvFake = findViewById(R.id.tvStrengthFake);
        Button btnUpdate = findViewById(R.id.btnUpdatePass);

        SharedPreferences prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);

        // Listener to monitor the strength of the new original PIN
        etNewOriginal.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString(), barOriginal, tvOriginal);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Listener to monitor the strength of the new fake PIN
        etNewFake.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString(), barFake, tvFake);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Logic for the Update button click
        btnUpdate.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newOriginal = etNewOriginal.getText().toString().trim();
            String newFake = etNewFake.getText().toString().trim();
            
            String savedEncryptedPass = prefs.getString("original_pin", "");

            if (oldPass.isEmpty() || newOriginal.isEmpty() || newFake.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Verifying if the current password provided by the user matches the saved hash
                String encryptedOld = CryptoStego.encrypt(oldPass, "AppInternalSecurityKey");

                if (!encryptedOld.equals(savedEncryptedPass)) {
                    Toast.makeText(this, "Current password is incorrect!", Toast.LENGTH_LONG).show();
                    etOldPass.setError("Incorrect password");
                    return;
                }

                // Ensuring the new PIN meets the minimum length requirement
                if (newOriginal.length() < 4) {
                    Toast.makeText(this, "PIN must be at least 4 characters long", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Security check: Both PINs must be different to avoid confusion
                if (newOriginal.equals(newFake)) {
                    Toast.makeText(this, "Original and Fake PINs must be different", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Encrypting and saving the new credentials locally
                String encNewOriginal = CryptoStego.encrypt(newOriginal, "AppInternalSecurityKey");
                String encNewFake = CryptoStego.encrypt(newFake, "AppInternalSecurityKey");

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("original_pin", encNewOriginal);
                editor.putString("fake_pin", encNewFake);
                editor.apply();

                Toast.makeText(this, "Security settings successfully updated!", Toast.LENGTH_LONG).show();
                finish();

            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to calculate and display the strength level of a password
    private void updateStrengthMeter(String password, ProgressBar bar, TextView label) {
        int strength = 0;
        if (password.length() >= 4) strength += 25;
        if (password.matches(".*[a-zA-Z].*")) strength += 25;
        if (password.matches(".*[0-9].*")) strength += 25;
        if (password.matches(".*[!@#$%^&*+=].*")) strength += 25;

        bar.setProgress(strength);
        if (strength <= 25) { 
            label.setText("Strength: Weak"); 
            label.setTextColor(Color.RED); 
        }
        else if (strength <= 75) { 
            label.setText("Strength: Fair"); 
            label.setTextColor(Color.parseColor("#FFA500")); 
        }
        else { 
            label.setText("Strength: Very Strong");
            label.setTextColor(Color.parseColor("#0D47A1")); 
        }
    }
}
