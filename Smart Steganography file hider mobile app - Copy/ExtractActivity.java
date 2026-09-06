package com.example.smartstego;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Yeh screen chupi hui files ko wapis nikalne (Extract) ke liye banayi gayi hai.
 * User image ya audio select karta hai, password dalta hai aur file bahar aa jati hai.
 */
public class ExtractActivity extends AppCompatActivity {

    // Buttons aur text views ke liye variables
    private Button btnSelectImage, btnSelectAudio, btnStartExtract;
    private TextView txtSelectedFileName;
    private EditText etPassword;
    private Uri mediaUri; // Selected file ka path store karne ke liye
    private boolean isImageMode = true; // Check karne ke liye ke image select hui hai ya audio

    // Gallery se file pick karne wala modern tareeqa
    private final ActivityResultLauncher<Intent> pickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mediaUri = result.getData().getData();
                    String name = FileUtils.getFileName(this, mediaUri);
                    txtSelectedFileName.setText("Selected: " + name);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract);

        // XML elements ko initialize kar rahe hain
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnStartExtract = findViewById(R.id.btnStartExtract);
        txtSelectedFileName = findViewById(R.id.txtSelectedFileName);
        etPassword = findViewById(R.id.etPassword);

        // Image select karne ka listener
        btnSelectImage.setOnClickListener(v -> {
            isImageMode = true;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickerLauncher.launch(intent);
        });

        // Audio select karne ka listener
        btnSelectAudio.setOnClickListener(v -> {
            isImageMode = false;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimes = {"audio/*", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
            pickerLauncher.launch(intent);
        });

        // "Decode & Restore" button ka kaam
        btnStartExtract.setOnClickListener(v -> {
            String pass = etPassword.getText().toString().trim();
            if (mediaUri == null || pass.isEmpty()) {
                Toast.makeText(this, "Pehle file select karein aur password likhein", Toast.LENGTH_SHORT).show();
                return;
            }
            // Extraction ka process shuru karna
            performExtraction(pass);
        });
    }

    // Background thread mein data nikalne ka kaam (taakay app hang na ho)
    private void performExtraction(String pass) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Data nikala ja raha hai... Sabar karein.");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                byte[] extractedBundle = null;

                if (isImageMode) {
                    // Image se data decode karna
                    InputStream is = getContentResolver().openInputStream(mediaUri);
                    if (is == null) throw new Exception("File nahi mil saki.");
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false; // Important: scaling band karni hai warna bits kharab ho jayengi
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap stegoBitmap = BitmapFactory.decodeStream(is, null, options);
                    is.close();
                    
                    if (stegoBitmap != null) {
                        extractedBundle = CryptoStego.decodeImageLSB(stegoBitmap);
                        stegoBitmap.recycle();
                    }
                } else {
                    // Audio se data decode karna
                    InputStream is = getContentResolver().openInputStream(mediaUri);
                    if (is == null) throw new Exception("File nahi mil saki.");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    byte[] audioBytes = bos.toByteArray();
                    is.close();
                    extractedBundle = CryptoStego.decodeAudioLSB(audioBytes);
                }

                // Check ke kia waqai is file mein koi secret data hai?
                if (extractedBundle == null) {
                    throw new Exception("Is file mein koi chupa hua data nahi mila!");
                }

                // Header se filename aur encrypted data alag karna
                int nameLen = extractedBundle[0] & 0xFF;
                String originalFileName = new String(extractedBundle, 1, nameLen, StandardCharsets.UTF_8);
                byte[] encryptedData = Arrays.copyOfRange(extractedBundle, 1 + nameLen, extractedBundle.length);

                // Data ko decrypt aur decompress karna (Asli halat mein lane ke liye)
                byte[] decryptedCompressed;
                try {
                    decryptedCompressed = CryptoStego.decryptBytes(encryptedData, pass);
                } catch (Exception e) {
                    throw new Exception("Ghalat Password! Dobara koshish karein.");
                }
                
                byte[] finalData = CryptoStego.decompress(decryptedCompressed);

                // File ko Downloads mein save karna
                Uri savedUri = FileUtils.saveRecoveredFile(this, finalData, originalFileName);

                runOnUiThread(() -> {
                    pd.dismiss();
                    if (savedUri != null) {
                        // History aur Activity Log mein add karna
                        RecentManager.addRecentItem(this, originalFileName, "Extracted", savedUri.toString());
                        Toast.makeText(this, "Success! File mil gayi.", Toast.LENGTH_LONG).show();
                        // File ko fauran open kar dena
                        openFileDirectly(savedUri, originalFileName);
                        finish();
                    } else {
                        Toast.makeText(this, "File save karne mein masla hua.", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // File ko uske relevant app mein kholne ka logic
    private void openFileDirectly(Uri uri, String fileName) {
        try {
            // System se pooch rahe hain ke is file ka type kia hai (PDF, JPG etc)
            ContentResolver resolver = getContentResolver();
            String mimeType = resolver.getType(uri);
            
            if (mimeType == null || mimeType.equals("application/octet-stream")) {
                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot != -1) {
                    extension = fileName.substring(lastDot + 1).toLowerCase();
                }
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            
            if (mimeType == null) mimeType = "*/*";

            // Intent bana kar doosri apps ko permission dena aur file open karna
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Intent chooser = Intent.createChooser(intent, "File ko is app mein kholein:");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "File save ho gayi hai par koi open karne wali app nahi mili.", Toast.LENGTH_SHORT).show();
        }
    }
}
