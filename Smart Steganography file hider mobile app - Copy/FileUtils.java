package com.example.smartstego;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class handles all file-related operations, including saving images/audio 
 * to the gallery and opening extracted files in the system.
 */
public class FileUtils {

    // Saves the stego-image to the phone's gallery (Pictures/SmartStego folder)
    public static Uri saveBitmapToGallery(Context context, Bitmap bitmap, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".png");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            Uri imageUri;
            
            // Using Scoped Storage for Android 10+ and traditional path for older versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartStego");
                imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SmartStego");
                if (!directory.exists()) directory.mkdirs();
                File file = new File(directory, fileName + ".png");
                imageUri = Uri.fromFile(file);
            }
            
            if (imageUri != null) {
                try (OutputStream fos = context.getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
            }
            return imageUri;
        } catch (Exception e) { return null; }
    }

    // Saves the stego-audio to the Music/SmartStego folder
    public static Uri saveAudioToStorage(Context context, byte[] audioData, String fileName, String extension) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + "." + extension);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
            Uri audioUri;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SmartStego");
                audioUri = context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SmartStego");
                if (!directory.exists()) directory.mkdirs();
                File file = new File(directory, fileName + "." + extension);
                audioUri = Uri.fromFile(file);
            }
            
            if (audioUri != null) {
                try (OutputStream fos = context.getContentResolver().openOutputStream(audioUri)) {
                    fos.write(audioData);
                }
            }
            return audioUri;
        } catch (Exception e) { return null; }
    }

    // Saves an encrypted copy of the secret data in the app's private folder for safety
    public static String saveInternalSecret(Context context, byte[] encryptedData, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), "vault");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName + ".bin");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(encryptedData);
            }
            return file.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    // Reads the encrypted secret from the app's internal vault
    public static byte[] readInternalSecret(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }
            return data;
        } catch (Exception e) { return null; }
    }

    // Saves the restored file to the Downloads folder after successful extraction
    public static Uri saveRecoveredFile(Context context, byte[] fileData, String fileName) {
        try {
            String extension = "";
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot != -1) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) mimeType = "application/octet-stream";

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Extracted_" + fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartStego");

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Using IS_PENDING to lock the file while writing to prevent corruption
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SmartStego");
                if (!dir.exists()) dir.mkdirs();
                uri = Uri.fromFile(new File(dir, "Extracted_" + fileName));
            }

            if (uri != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        os.write(fileData);
                        os.flush();
                    }
                }
                
                // Finalize the file and release it for other applications
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                } else {
                    MediaScannerConnection.scanFile(context, new String[]{uri.getPath()}, null, null);
                }
                
                // Refresh the system database so the file is visible in Gallery/File Manager
                context.getContentResolver().notifyChange(uri, null);
                return uri;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Opens a file using the system's default viewer (e.g., Photos for images, PDF reader for documents)
    public static void openFile(Context context, Uri uri, String fileName) {
        try {
            // Accurately determining the file type using the ContentResolver
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null || mimeType.equals("application/octet-stream")) {
                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot != -1 && lastDot < fileName.length() - 1) {
                    extension = fileName.substring(lastDot + 1).toLowerCase();
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }
            }
            if (mimeType == null) mimeType = "*/*";

            // Creating an intent to view the file with appropriate read permissions
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(Intent.createChooser(intent, "Open file with:"));
        } catch (Exception e) {
            Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_SHORT).show();
        }
    }

    // Extracts the display name from a Content Uri
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
}
