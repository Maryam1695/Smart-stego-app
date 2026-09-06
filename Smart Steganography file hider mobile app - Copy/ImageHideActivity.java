package com.example.smartstego;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Is activity mein hum image ke andar secret files hide karne ka kaam karte hain.
 * User image aur apni secret file select karta hai, phir password set karke unhe merge kar deta hai.
 */
public class ImageHideActivity extends AppCompatActivity {

    // UI ke widgets declare kar rahe hain
    private Button btnSelectImage, btnSelectFile, btnStartHiding;
    private ImageView imgPreview;
    private TextView txtFileName;
    private EditText etPassword;

    private Uri imageUri; // Carrier image ka path
    private Uri secretFileUri; // Jo file chupani hai uska path
    private String secretFileName = "secret_file";

    // Gallery se base image (carrier) uthane ke liye launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    // Image select hote hi uska preview screen par dikhana
                    imgPreview.setImageURI(imageUri);
                    imgPreview.setAlpha(1.0f);
                    imgPreview.setPadding(0, 0, 0, 0);
                    imgPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            });

    // Kisi bhi format ki secret file pick karne ke liye launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    secretFileUri = result.getData().getData();
                    // File ka naam aur size screen par show karna
                    updateSecretFileInfo();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_hide);

        // Buttons aur inputs ko XML se connect kar rahe hain
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnStartHiding = findViewById(R.id.btnStartHiding);
        imgPreview = findViewById(R.id.imgPreview);
        txtFileName = findViewById(R.id.txtFileName);
        etPassword = findViewById(R.id.etPassword);

        // Image select karne ka button
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Secret document select karne ka button
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Har tarah ki file support hogi
            filePickerLauncher.launch(intent);
        });

        // "Start Hiding" par click hone par validations aur hiding process
        btnStartHiding.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (imageUri == null || secretFileUri == null || password.isEmpty()) {
                Toast.makeText(this, "Meharbani karke saari fields fill karein!", Toast.LENGTH_SHORT).show();
                return;
            }
            startHidingProcess(password);
        });
    }

    // Selected file ka metadata (naam, size) nikalne ka tareeqa
    private void updateSecretFileInfo() {
        try (Cursor cursor = getContentResolver().query(secretFileUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                secretFileName = cursor.getString(nameIdx);
                long size = cursor.getLong(sizeIdx);
                // Size ko KB ya MB mein convert karke dikhana
                String sizeStr = size < 1024 * 1024 ? (size/1024)+" KB" : String.format(Locale.US, "%.1f MB", size/(1024.0*1024.0));
                txtFileName.setText("Chunni gayi file: " + secretFileName + " (" + sizeStr + ")");
            }
        } catch (Exception ignored) {}
    }

    // Actual Steganography ka heavy process background thread mein
    private void startHidingProcess(String password) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Data secure kiya ja raha hai...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                // 1. Secret file ko bytes mein read karna
                InputStream fileIs = getContentResolver().openInputStream(secretFileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int read;
                while ((read = fileIs.read(buf)) != -1) bos.write(buf, 0, read);
                byte[] secretBytes = bos.toByteArray();
                fileIs.close();

                // 2. Data ko compress aur encrypt karna (Security Layer)
                byte[] compressedBytes = CryptoStego.compress(secretBytes);
                byte[] encryptedBytes = CryptoStego.encryptBytes(compressedBytes, password);

                // 3. Carrier image ko decode karna (Scaling off rakhi hai taakay pixels na badlein)
                InputStream imgIs = getContentResolver().openInputStream(imageUri);
                BitmapFactory.Options loadOptions = new BitmapFactory.Options();
                loadOptions.inScaled = false;
                loadOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeStream(imgIs, null, loadOptions);
                imgIs.close();

                if (bitmap == null) throw new Exception("Tasveer load nahi ho saki.");
                
                // Capacity check: Dekhna ke data image mein fit ayega ya nahi
                if ((encryptedBytes.length + 500) > CryptoStego.getImageCapacity(bitmap)) {
                    throw new Exception("File bahut bari hai, koi badi HD tasveer istemal karein.");
                }

                // 4. LSB Algorithm ke zariye bits hide karna
                Bitmap stegoBitmap = CryptoStego.encodeImageLSB(bitmap, encryptedBytes, secretFileName);
                
                // 5. Resulting stego-image ko gallery mein save karna
                String outFileName = "ImgStego_" + System.currentTimeMillis();
                Uri savedUri = FileUtils.saveBitmapToGallery(this, stegoBitmap, outFileName);
                
                // 6. Reliability ke liye aik encrypted copy internal vault mein bhi rakhna
                String internalPath = FileUtils.saveInternalSecret(this, encryptedBytes, outFileName);

                // Resources release karna
                if (bitmap != stegoBitmap) bitmap.recycle();
                stegoBitmap.recycle();

                // UI update karna process khatam hone par
                runOnUiThread(() -> {
                    pd.dismiss();
                    if (savedUri != null) {
                        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                        // History mein record save karna
                        HistoryItem item = new HistoryItem(outFileName + ".png", "Image", date, savedUri.toString(), internalPath);
                        HistoryManager.saveHistoryItem(this, item);
                        
                        // Recent logs mein update karna
                        RecentManager.addRecentItem(this, outFileName + ".png", "Embedded", savedUri.toString());

                        Toast.makeText(this, "Mubarak ho! Data kamiyabi se chupa diya gaya.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Save karne mein masla hua.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    new AlertDialog.Builder(this).setTitle("Ghalti").setMessage(e.getMessage()).setPositiveButton("Theek hai", null).show();
                });
            }
        }).start();
    }
}
