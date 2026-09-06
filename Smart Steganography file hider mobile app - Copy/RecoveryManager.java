package com.example.smartstego;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * This class manages the entire backup and restore system of the app.
 * We designed it to be a "Universal Backup" where a single encrypted file (.enc) 
 * contains the PINs, history, and actual hidden data.
 */
public class RecoveryManager {

    // Internal key used to encrypt the backup file itself for extra security
    private static final String INTERNAL_BACKUP_KEY = "SmartStegoBackupSecurityKey2024";
    private static final String BACKUP_DIR_NAME = "smartstegobackup";
    private static final String BACKUP_FILE_NAME = "smartstego_universal_backup.enc";

    // Generates a random 12-digit key for account recovery
    public static String generateShortKey() {
        Random r = new Random();
        return String.format(Locale.US, "%04d-%04d-%04d", 
                r.nextInt(10000), r.nextInt(10000), r.nextInt(10000));
    }

    // Creates the initial auto-backup during the setup process
    public static void saveAutoBackup(Context context, String recoveryKey, String originalPin, String fakePin, String ans1, String ans2) {
        try {
            JSONObject backupData = new JSONObject();
            backupData.put("recovery_key", recoveryKey);
            backupData.put("original_pin", originalPin);
            backupData.put("fake_pin", fakePin);
            backupData.put("ans1", ans1);
            backupData.put("ans2", ans2);
            // Including the history and vault data in the backup for total reliability
            backupData.put("history", buildHistoryWithVault(context));
            backupData.put("creation_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            // Writing the encrypted data bundle to a file
            writeBackupToFile(context, backupData.toString());
        } catch (Exception e) {
            Log.e("RecoveryManager", "Backup failed", e);
        }
    }

    // Synchronizes the backup file whenever history or security settings change
    public static void syncBackupWithHistory(Context context, String historyJson) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);
            String encMaster = prefs.getString("master_recovery_key", "");
            if (encMaster.isEmpty()) return;

            String rawKey = CryptoStego.decrypt(encMaster, "AppInternalSecurityKey");
            JSONObject backupData = new JSONObject();
            backupData.put("recovery_key", rawKey);
            backupData.put("original_pin", prefs.getString("original_pin", ""));
            backupData.put("fake_pin", prefs.getString("fake_pin", ""));
            backupData.put("ans1", prefs.getString("recovery_answer1", ""));
            backupData.put("ans2", prefs.getString("recovery_answer2", ""));
            backupData.put("history", buildHistoryWithVault(context));
            backupData.put("creation_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            writeBackupToFile(context, backupData.toString());
        } catch (Exception ignored) {}
    }

    // Packs the encrypted bytes of hidden files into the backup JSON for redundancy
    private static String buildHistoryWithVault(Context context) throws Exception {
        JSONArray history = new JSONArray(HistoryManager.getHistoryAsJson(context));
        for (int i = 0; i < history.length(); i++) {
            JSONObject item = history.getJSONObject(i);
            String internalPath = item.optString("internalPath", "");
            if (!internalPath.isEmpty()) {
                // Reading the actual encrypted secret copy
                byte[] data = FileUtils.readInternalSecret(internalPath);
                if (data != null) {
                    // Storing binary data as a Base64 string in the JSON
                    item.put("vaultData", Base64.encodeToString(data, Base64.NO_WRAP));
                }
            }
        }
        return history.toString();
    }

    // Logic to write the encrypted backup file to the phone's public Downloads folder
    private static void writeBackupToFile(Context context, String dataJson) throws Exception {
        byte[] encryptedData = CryptoStego.encryptBytes(dataJson.getBytes(StandardCharsets.UTF_8), INTERNAL_BACKUP_KEY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ logic using MediaStore
            context.getContentResolver().delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, 
                    MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                    new String[]{BACKUP_FILE_NAME, Environment.DIRECTORY_DOWNLOADS + "/" + BACKUP_DIR_NAME + "/"});

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + BACKUP_DIR_NAME);
            
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os != null) os.write(encryptedData);
                }
            }
        } else {
            // Traditional file handling for older Android versions
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_DIR_NAME);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, BACKUP_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(encryptedData);
            }
        }
    }

    // Restores the entire app state from a selected .enc backup file
    public static boolean restoreFromUri(Context context, Uri uri, String key) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) bos.write(buffer, 0, read);
            return verifyAndRestore(context, bos.toByteArray(), key);
        } catch (Exception e) {
            return false;
        }
    }

    // Verifies the recovery key and decrypts the backup data bundle
    private static boolean verifyAndRestore(Context context, byte[] encryptedData, String cleanKey) throws Exception {
        try {
            byte[] decryptedBytes = CryptoStego.decryptBytes(encryptedData, INTERNAL_BACKUP_KEY);
            JSONObject json = new JSONObject(new String(decryptedBytes, StandardCharsets.UTF_8));
            String savedKey = json.getString("recovery_key").replace("-", "").trim();
            
            // Check if the entered key matches the one stored inside the backup file
            if (cleanKey.replace("-", "").equalsIgnoreCase(savedKey.replace("-", ""))) {
                SharedPreferences prefs = context.getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("original_pin", json.optString("original_pin", ""));
                editor.putString("fake_pin", json.optString("fake_pin", ""));
                editor.putString("recovery_answer1", json.optString("ans1", ""));
                editor.putString("recovery_answer2", json.optString("ans2", ""));
                
                // Restoring the history records and physical hidden files
                restoreVaultFromHistory(context, json.optString("history", "[]"));

                editor.putString("master_recovery_key", CryptoStego.encrypt(cleanKey, "AppInternalSecurityKey"));
                editor.apply();
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // Unpacks Base64 data from history and recreates the files in the internal vault
    private static void restoreVaultFromHistory(Context context, String historyJson) throws Exception {
        JSONArray array = new JSONArray(historyJson);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String vaultData = item.optString("vaultData", "");
            String name = item.getString("name");
            if (!vaultData.isEmpty()) {
                byte[] data = Base64.decode(vaultData, Base64.NO_WRAP);
                // Saving the bytes back to the app's internal "Vault" folder
                String internalPath = FileUtils.saveInternalSecret(context, data, name.substring(0, name.lastIndexOf('.')));
                item.put("internalPath", internalPath);
                item.remove("vaultData"); // Clean up large binary string from memory
            }
        }
        HistoryManager.restoreHistoryFromJson(context, array.toString());
    }
}
