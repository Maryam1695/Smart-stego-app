package com.example.smartstego;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.nio.charset.StandardCharsets;

/**
 * This activity allows the user to update their security PIN and recovery answers.
 * We ensure that when the PIN is changed, the external backup file is also synchronized.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Binding XML components to Java variables
        TextInputEditText etOldPass = findViewById(R.id.etOldPass);
        TextInputEditText etNewOriginal = findViewById(R.id.etNewOriginalPass);
        TextInputEditText etNewFake = findViewById(R.id.etNewFakePass);
        TextInputEditText etNewAns1 = findViewById(R.id.etNewSecurityAnswer1);
        TextInputEditText etNewAns2 = findViewById(R.id.etNewSecurityAnswer2);

        ProgressBar barOriginal = findViewById(R.id.barNewOriginal);
        ProgressBar barFake = findViewById(R.id.barNewFake);
        TextView tvOriginal = findViewById(R.id.tvStrengthOriginal);
        TextView tvFake = findViewById(R.id.tvStrengthFake);
        Button btnUpdate = findViewById(R.id.btnUpdatePass);

        SharedPreferences prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);

        // Real-time strength meter for the new Original PIN
        if (etNewOriginal != null) {
            etNewOriginal.addTextChangedListener(new TextWatcher() {
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateStrengthMeter(s.toString(), barOriginal, tvOriginal);
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Real-time strength meter for the new Fake PIN
        if (etNewFake != null) {
            etNewFake.addTextChangedListener(new TextWatcher() {
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateStrengthMeter(s.toString(), barFake, tvFake);
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Logic to validate and save the new security settings
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                String oldPass = etOldPass.getText().toString().trim();
                String newOriginal = etNewOriginal.getText().toString().trim();
                String newFake = etNewFake.getText().toString().trim();
                String ans1 = etNewAns1.getText().toString().trim().toLowerCase();
                String ans2 = etNewAns2.getText().toString().trim().toLowerCase();
                
                String savedEncPass = prefs.getString("original_pin", "");

                // Basic validation to ensure all fields are filled
                if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newOriginal) || TextUtils.isEmpty(newFake) || 
                    TextUtils.isEmpty(ans1) || TextUtils.isEmpty(ans2)) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    // 1. Verify that the current password is correct before allowing changes
                    String encryptedOld = CryptoStego.encrypt(oldPass, "AppInternalSecurityKey");
                    if (!encryptedOld.equals(savedEncPass)) {
                        etOldPass.setError("Incorrect current password");
                        return;
                    }

                    // 2. Encrypt the new credentials for secure storage
                    String encNewOriginal = CryptoStego.encrypt(newOriginal, "AppInternalSecurityKey");
                    String encNewFake = CryptoStego.encrypt(newFake, "AppInternalSecurityKey");
                    String encAns1 = CryptoStego.encrypt(ans1, "AppInternalSecurityKey");
                    String encAns2 = CryptoStego.encrypt(ans2, "AppInternalSecurityKey");

                    // 3. Save the encrypted data to local storage (SharedPreferences)
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("original_pin", encNewOriginal);
                    editor.putString("fake_pin", encNewFake);
                    editor.putString("recovery_answer1", encAns1);
                    editor.putString("recovery_answer2", encAns2);
                    editor.apply();

                    // 4. Important: Update the Universal Backup file to match new settings
                    String savedMasterKeyEnc = prefs.getString("master_recovery_key", "");
                    if (!savedMasterKeyEnc.isEmpty()) {
                        String rawKey = CryptoStego.decrypt(savedMasterKeyEnc, "AppInternalSecurityKey");
                        RecoveryManager.saveAutoBackup(this, rawKey, encNewOriginal, encNewFake, encAns1, encAns2);
                    }

                    Toast.makeText(this, "Security settings successfully updated!", Toast.LENGTH_LONG).show();
                    finish();

                } catch (Exception e) {
                    Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Function to calculate and display password strength
    private void updateStrengthMeter(String password, ProgressBar bar, TextView label) {
        if (bar == null || label == null) return;
        int strength = 0;
        if (password.length() >= 4) strength += 25;
        if (password.matches(".*[a-zA-Z].*")) strength += 25;
        if (password.matches(".*[0-9].*")) strength += 25;
        if (password.matches(".*[!@#$%^&*+=].*")) strength += 25;
        
        bar.setProgress(strength);
        // Updating color and text based on calculated strength
        int color = strength <= 25 ? Color.RED : (strength <= 75 ? Color.parseColor("#FFA500") : Color.parseColor("#0D47A1"));
        String text = strength <= 25 ? "Weak" : (strength <= 75 ? "Medium" : "Strong");
        
        label.setText("Strength: " + text);
        label.setTextColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bar.setProgressTintList(ColorStateList.valueOf(color));
        }
    }
}
