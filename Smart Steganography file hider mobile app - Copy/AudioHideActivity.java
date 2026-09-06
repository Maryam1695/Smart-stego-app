package com.example.smartstego;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Yeh activity audio files (WAV) ke andar secret data hide karne ke liye hai.
 * Humne isme logic lagaya hai ke pehle koi bhi audio WAV mein convert ho, phir usme data chupaaya jaye.
 */
public class AudioHideActivity extends AppCompatActivity {

    // UI elements ke variables
    private Button btnSelectAudio, btnSelectFile, btnStartHiding;
    private TextView txtAudioFileName, txtSecretFileName;
    private EditText etPassword;

    private Uri audioUri; // Base audio ka path
    private Uri secretFileUri; // Jo file chupani hai uska path
    private String secretFileName = "secret_file";

    // Audio file select karne wala picker
    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    audioUri = result.getData().getData();
                    String name = getFileName(audioUri);
                    // Label update kar rahe hain taakay pata chale konsi audio chunni gayi hai
                    txtAudioFileName.setText("Selected Audio: " + name);
                }
            });

    // Secret file select karne wala picker
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    secretFileUri = result.getData().getData();
                    // Chunni gayi file ki details update karna
                    updateSecretFileInfo();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_hide);

        // Buttons aur Views ko XML id se connect karna
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnStartHiding = findViewById(R.id.btnStartHiding);
        txtAudioFileName = findViewById(R.id.txtAudioFileName);
        txtSecretFileName = findViewById(R.id.txtSecretFileName);
        etPassword = findViewById(R.id.etPassword);

        // Audio select button ka click listener
        btnSelectAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            audioPickerLauncher.launch(intent);
        });

        // Secret file select button ka click listener
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Har tarah ki file support hogi
            filePickerLauncher.launch(intent);
        });

        // Hiding process shuru karne wala button
        btnStartHiding.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            // Basic validation ke koi cheez khali na reh jaye
            if (audioUri == null || secretFileUri == null || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Pehle audio convert hogi phir hide
            processHiding(password);
        });
    }

    // Selected secret file ka naam aur size nikalne ka tareeqa
    private void updateSecretFileInfo() {
        try (Cursor cursor = getContentResolver().query(secretFileUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                secretFileName = cursor.getString(nameIdx);
                long size = cursor.getLong(sizeIdx);
                String sizeStr = size < 1024 * 1024 ? (size/1024)+" KB" : String.format(Locale.US, "%.1f MB", size/(1024.0*1024.0));
                txtSecretFileName.setText("Selected File: " + secretFileName + " (" + sizeStr + ")");
            }
        } catch (Exception ignored) {}
    }

    // Audio ko WAV mein badalne aur hiding function ko call karne ka kaam
    private void processHiding(String password) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Securing data...");
        pd.setCancelable(false);
        pd.show();

        // Audio converter use kar rahe hain WAV ke liye
        AudioConverter.convertToWav(this, audioUri, new AudioConverter.ConversionCallback() {
            @Override
            public void onSuccess(File convertedWav) {
                // Conversion kamiyab hui, ab asli hiding shuru
                executeHiding(convertedWav, password, pd);
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(AudioHideActivity.this, "Processing failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Background thread mein LSB embedding aur encryption ka kaam
    private void executeHiding(File convertedWav, String password, ProgressDialog pd) {
        new Thread(() -> {
            try {
                // 1. Secret file ko bytes mein read karna
                InputStream fileIs = getContentResolver().openInputStream(secretFileUri);
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int read;
                while ((read = fileIs.read(buf)) != -1) bos.write(buf, 0, read);
                byte[] secretBytes = bos.toByteArray();
                fileIs.close();

                // 2. Compress aur Encrypt karna taakay security rahay
                byte[] compressedBytes = CryptoStego.compress(secretBytes);
                byte[] encryptedBytes = CryptoStego.encryptBytes(compressedBytes, password);

                // 3. Converted WAV audio ko bytes mein lena
                long wavSize = convertedWav.length();
                byte[] audioBytes = new byte[(int) wavSize];
                try (FileInputStream fis = new FileInputStream(convertedWav)) {
                    fis.read(audioBytes);
                }
                if (convertedWav.exists()) convertedWav.delete(); // Temp file saaf karna

                // 4. Capacity check karna
                if ((encryptedBytes.length + 500) > CryptoStego.getAudioCapacity(audioBytes)) {
                    throw new Exception("File is too large for this audio.");
                }

                // 5. Asli LSB logic call karna (Hiding)
                byte[] stegoAudio = CryptoStego.encodeAudioLSB(audioBytes, encryptedBytes, secretFileName);
                String outFileName = "AudStego_" + System.currentTimeMillis();
                
                // 6. Result ko phone ki storage mein save karna
                Uri savedUri = FileUtils.saveAudioToStorage(this, stegoAudio, outFileName, "wav");

                // 7. Internal vault mein safety copy rakhna
                String internalPath = FileUtils.saveInternalSecret(this, encryptedBytes, outFileName);

                runOnUiThread(() -> {
                    pd.dismiss();
                    if (savedUri != null) {
                        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                        // History aur Recent Activity update karna
                        HistoryItem item = new HistoryItem(outFileName + ".wav", "Audio", date, savedUri.toString(), internalPath);
                        HistoryManager.saveHistoryItem(this, item);
                        RecentManager.addRecentItem(this, outFileName + ".wav", "Embedded", savedUri.toString());

                        Toast.makeText(this, "Success! File hidden in Storage & Vault.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    new AlertDialog.Builder(this).setTitle("Error").setMessage(e.getMessage()).setPositiveButton("OK", null).show();
                });
            }
        }).start();
    }

    // URI se file ka naam nikalne ka helper function
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception ignored) {}
        }
        return result != null ? result : uri.getLastPathSegment();
    }
}
