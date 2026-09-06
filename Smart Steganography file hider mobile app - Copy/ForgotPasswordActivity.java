package com.example.smartstego;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * This activity handles PIN recovery if the user forgets their password.
 * We provide two options: answering security questions or using a Master Recovery Key.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    // UI layouts and button variables
    private LinearLayout layoutQuestions, layoutMasterKey;
    private TextInputEditText etAnswer1, etAnswer2, etMasterKeyInput;
    private Button btnReset, btnVerifyMasterKey, btnPickBackupFile;
    private TextView tvUseRecoveryKey, tvBackToQuestions;

    // Launcher for selecting the .enc backup file from storage
    private final ActivityResultLauncher<String[]> backupFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) showKeyInputForManualFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initializing XML elements
        layoutQuestions = findViewById(R.id.layoutSecurityQuestions);
        layoutMasterKey = findViewById(R.id.layoutMasterKey);
        etAnswer1 = findViewById(R.id.etSecurityAnswer1);
        etAnswer2 = findViewById(R.id.etSecurityAnswer2);
        etMasterKeyInput = findViewById(R.id.etMasterKey);
        btnReset = findViewById(R.id.btnResetPassword);
        btnVerifyMasterKey = findViewById(R.id.btnVerifyMasterKey);
        tvUseRecoveryKey = findViewById(R.id.tvUseRecoveryKey);
        tvBackToQuestions = findViewById(R.id.tvBackToQuestions);
        
        // Dynamically adding a button to pick the backup file
        btnPickBackupFile = new Button(this);
        btnPickBackupFile.setText("Restore from Backup File (.enc)");
        btnPickBackupFile.setAllCaps(false);
        layoutMasterKey.addView(btnPickBackupFile, 2);

        SharedPreferences prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);

        // Switch from questions view to recovery key view
        tvUseRecoveryKey.setOnClickListener(v -> {
            layoutQuestions.setVisibility(View.GONE);
            layoutMasterKey.setVisibility(View.VISIBLE);
        });

        // Switch back to security questions view
        tvBackToQuestions.setOnClickListener(v -> {
            layoutMasterKey.setVisibility(View.GONE);
            layoutQuestions.setVisibility(View.VISIBLE);
        });

        // Open the file picker to find the backup
        btnPickBackupFile.setOnClickListener(v -> backupFilePickerLauncher.launch(new String[]{"application/octet-stream", "*/*"}));

        // Reset password by verifying security answers
        btnReset.setOnClickListener(v -> {
            String ans1 = etAnswer1.getText().toString().trim().toLowerCase();
            String ans2 = etAnswer2.getText().toString().trim().toLowerCase();
            String savedEnc1 = prefs.getString("recovery_answer1", "");
            String savedEnc2 = prefs.getString("recovery_answer2", "");

            try {
                // Encrypting answers to match the stored hashes for security
                if (CryptoStego.encrypt(ans1, "AppInternalSecurityKey").equals(savedEnc1) && 
                    CryptoStego.encrypt(ans2, "AppInternalSecurityKey").equals(savedEnc2)) {
                    // Redirect to setup to create new PINs
                    goToSetup();
                } else {
                    Toast.makeText(this, "Incorrect answers! Please try again.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) { Toast.makeText(this, "Security Error", Toast.LENGTH_SHORT).show(); }
        });

        // Manual verification of the master key
        btnVerifyMasterKey.setOnClickListener(v -> {
            Toast.makeText(this, "Use 'Pick Backup File' for a secure restoration.", Toast.LENGTH_LONG).show();
        });
    }

    // After picking a file, ask the user for the 12-digit key to unlock it
    private void showKeyInputForManualFile(Uri fileUri) {
        View view = getLayoutInflater().inflate(R.layout.dialog_restore, null);
        TextInputEditText etKey = view.findViewById(R.id.etRestoreKey);
        Button btnRestore = view.findViewById(R.id.btnRestoreSubmit);
        
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view);
        
        btnRestore.setOnClickListener(v -> {
            String enteredKey = etKey.getText().toString().trim().toUpperCase();
            // Using RecoveryManager to decode and restore from the selected file
            if (RecoveryManager.restoreFromUri(this, fileUri, enteredKey)) {
                dialog.dismiss();
                handleSuccess();
            } else {
                Toast.makeText(this, "Invalid Key! This file could not be decoded.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    // Success callback after account restoration
    private void handleSuccess() {
        Toast.makeText(this, "Success! Account Restored.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Navigate to the Setup screen to create new PINs
    private void goToSetup() {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
