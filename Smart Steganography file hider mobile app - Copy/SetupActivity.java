package com.example.smartstego;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * This screen appears when the user installs the app for the first time.
 * Its purpose is to let the user set up their security PINs and recovery questions.
 */
public class SetupActivity extends AppCompatActivity {

    // Launcher to select a backup file from the phone's storage
    private final ActivityResultLauncher<String[]> backupFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) showKeyInputForManualFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Binding XML elements to Java variables
        TextInputEditText etOriginalPass = findViewById(R.id.etOriginalPass);
        TextInputEditText etFakePass = findViewById(R.id.etFakePass);
        TextInputEditText etAnswer1 = findViewById(R.id.etSecurityAnswer1);
        TextInputEditText etAnswer2 = findViewById(R.id.etSecurityAnswer2);
        
        ProgressBar barOriginal = findViewById(R.id.strengthBarOriginal);
        ProgressBar barFake = findViewById(R.id.strengthBarFake);
        TextView tvOriginal = findViewById(R.id.tvStrengthOriginal);
        TextView tvFake = findViewById(R.id.tvStrengthFake);
        
        Button btnSave = findViewById(R.id.btnSaveSetup);
        TextView tvRestore = findViewById(R.id.tvRestoreAccount);

        // Monitoring password strength as the user types the Original PIN
        etOriginalPass.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString(), barOriginal, tvOriginal);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Monitoring password strength for the Fake PIN
        etFakePass.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString(), barFake, tvFake);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Validating inputs and saving the encrypted setup data
        btnSave.setOnClickListener(v -> {
            String original = etOriginalPass.getText().toString().trim();
            String fake = etFakePass.getText().toString().trim();
            String ans1 = etAnswer1.getText().toString().trim().toLowerCase();
            String ans2 = etAnswer2.getText().toString().trim().toLowerCase();

            // Ensuring PINs meet the minimum security requirements
            if (original.length() < 4 || fake.length() < 4) {
                Toast.makeText(this, "PINs must be at least 4 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Generating a unique recovery key for the new account
                String recoveryKey = RecoveryManager.generateShortKey();
                
                // Encrypting all sensitive information before storing it locally
                String encOriginal = CryptoStego.encrypt(original, "AppInternalSecurityKey");
                String encFake = CryptoStego.encrypt(fake, "AppInternalSecurityKey");
                String encAns1 = CryptoStego.encrypt(ans1, "AppInternalSecurityKey");
                String encAns2 = CryptoStego.encrypt(ans2, "AppInternalSecurityKey");

                // Saving the encrypted credentials to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("original_pin", encOriginal);
                editor.putString("fake_pin", encFake);
                editor.putString("recovery_answer1", encAns1);
                editor.putString("recovery_answer2", encAns2);
                editor.putString("master_recovery_key", CryptoStego.encrypt(recoveryKey, "AppInternalSecurityKey"));
                editor.apply();

                // Triggering an automatic backup to the phone's storage
                RecoveryManager.saveAutoBackup(this, recoveryKey, encOriginal, encFake, encAns1, encAns2);
                
                // Showing the recovery key to the user so they can save it
                showRecoveryDialog(recoveryKey);
            } catch (Exception e) { 
                Toast.makeText(this, "Setup failed due to an internal error", Toast.LENGTH_SHORT).show(); 
            }
        });

        // Option for users who already have a backup file to restore their account
        tvRestore.setOnClickListener(v -> showRestoreOptionsDialog());
    }

    // Displays a dialog box containing the Master Recovery Key
    private void showRecoveryDialog(String key) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_recovery_info, null);
        TextView tvKey = view.findViewById(R.id.tvRecoveryKeyShort);
        Button btnDone = view.findViewById(R.id.btnRecoveryDone);
        Button btnCopy = view.findViewById(R.id.btnRecoveryCopy);
        
        tvKey.setText(key);
        
        // Listener to copy the recovery key to the system clipboard
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Recovery Key", key);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Recovery Key copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Proceed to the Login screen after the user acknowledges the key
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        dialog.show();
    }

    // Shows the restoration dialog to pick a backup file
    private void showRestoreOptionsDialog() {
        String[] options = {"Select Backup File (.enc)"};
        new AlertDialog.Builder(this)
                .setTitle("Restore Account")
                .setItems(options, (dialog, which) -> {
                    backupFilePickerLauncher.launch(new String[]{"application/octet-stream", "*/*"});
                })
                .show();
    }

    // Prompts the user for their 12-digit recovery key after they select a backup file
    private void showKeyInputForManualFile(Uri fileUri) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_restore, null);
        TextInputEditText etKey = view.findViewById(R.id.etRestoreKey);
        Button btnRestore = view.findViewById(R.id.btnRestoreSubmit);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view);

        btnRestore.setOnClickListener(v -> {
            String enteredKey = etKey.getText().toString().trim().toUpperCase();
            // Attempting to restore the account data using the selected file and key
            if (RecoveryManager.restoreFromUri(this, fileUri, enteredKey)) {
                dialog.dismiss();
                Toast.makeText(this, "Account successfully restored!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid Key! Restoration failed.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    // Logic to calculate and display the strength of a given password
    private void updateStrengthMeter(String password, ProgressBar bar, TextView label) {
        if (bar == null || label == null) return;
        int strength = 0;
        
        // Checking for different criteria: length, letters, numbers, and symbols
        if (password.length() >= 4) strength += 25;
        if (password.matches(".*[a-zA-Z].*")) strength += 25;
        if (password.matches(".*[0-9].*")) strength += 25;
        if (password.matches(".*[!@#$%^&*+=].*")) strength += 25;
        
        bar.setProgress(strength);
        
        // Changing colors and text based on the calculated strength percentage
        int color = (password.isEmpty()) ? Color.LTGRAY : (strength <= 25 ? Color.RED : (strength <= 75 ? Color.parseColor("#FFA500") : Color.parseColor("#0D47A1")));
        String text = (password.isEmpty()) ? "Empty" : (strength <= 25 ? "Weak" : (strength <= 75 ? "Medium" : "Strong"));
        
        label.setText("Strength: " + text);
        label.setTextColor(color);
        
        // Applying the color tint to the progress bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bar.setProgressTintList(ColorStateList.valueOf(color));
        }
    }
}
