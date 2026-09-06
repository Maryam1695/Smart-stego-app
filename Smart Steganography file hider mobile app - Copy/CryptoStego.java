package com.example.smartstego;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

/**
 * This class is the core engine of the application.
 * It contains the logic for encryption, compression, and steganography.
 */
public class CryptoStego {

    // AES encryption standard is used for data security
    private static final String ALGORITHM = "AES";
    
    // Magic Bytes used to identify if a file belongs to this application
    private static final byte[] MAGIC = "STG_V100".getBytes(StandardCharsets.UTF_8);

    // --- Compression Functions ---
    // Compresses the secret data to reduce its size before hiding
    public static byte[] compress(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.close();
        return bos.toByteArray();
    }

    // Decompresses the data during extraction to restore original size
    public static byte[] decompress(byte[] compressedData) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzip = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = gzip.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        gzip.close();
        return bos.toByteArray();
    }

    // --- Encryption Functions (Security Layer) ---
    // Encrypts string data using the user's password
    public static String encrypt(String data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encVal, Base64.NO_WRAP);
    }

    // Decrypts string data using the user's password
    public static String decrypt(String encryptedData, String password) throws Exception {
        byte[] decoded = Base64.decode(encryptedData, Base64.NO_WRAP);
        byte[] decrypted = decryptBytes(decoded, password);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // Encrypts byte data (files) for secure storage
    public static byte[] encryptBytes(byte[] data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(data);
    }

    // Decrypts byte data during extraction
    public static byte[] decryptBytes(byte[] encryptedData, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        return c.doFinal(encryptedData);
    }

    // Generates a 256-bit secure key from the password using SHA-256
    private static SecretKeySpec generateKey(String password) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, ALGORITHM);
    }

    // --- Capacity Check ---
    // Calculates how many bytes can be hidden in a specific bitmap
    public static long getImageCapacity(Bitmap bitmap) {
        if (bitmap == null) return 0;
        // Using 4-bit LSB (4 bits per channel) for maximum capacity
        return (long) (bitmap.getWidth() * bitmap.getHeight() * 3 * 4) / 8;
    }

    // Calculates the capacity of a WAV audio file
    public static long getAudioCapacity(byte[] audioData) {
        if (audioData == null || audioData.length <= 44) return 0;
        return (long) (audioData.length - 44) * 4 / 8;
    }

    // --- Image Steganography Logic ---
    // Encodes secret data into image pixels using 4-bit LSB
    public static Bitmap encodeImageLSB(Bitmap source, byte[] payload, String fileName) throws Exception {
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        int totalRequired = MAGIC.length + 4 + nameBytes.length + 4 + payload.length;

        // Check if the image has enough space for the secret data
        if (totalRequired > getImageCapacity(source)) {
            throw new Exception("File too large for this image. Try an HD image.");
        }

        // Combine Header, Filename, and actual Data
        ByteBuffer buffer = ByteBuffer.allocate(totalRequired);
        buffer.put(MAGIC);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(payload.length);
        buffer.put(payload);
        byte[] combined = buffer.array();

        int width = source.getWidth(), height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        int byteIdx = 0, bitIdx = 0;
        // Loop through pixels and replace last 4 bits of each RGB channel
        for (int i = 0; i < pixels.length && byteIdx < combined.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            int[] channels = {r, g, b};

            for (int c = 0; c < 3 && byteIdx < combined.length; c++) {
                int bits = (combined[byteIdx] >> bitIdx) & 0x0F;
                channels[c] = (channels[c] & 0xF0) | bits;
                bitIdx += 4;
                if (bitIdx == 8) { bitIdx = 0; byteIdx++; }
            }
            pixels[i] = (p & 0xFF000000) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
        }

        // Create a new bitmap with the hidden data
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    // Decodes and extracts the hidden data from a stego-image
    public static byte[] decodeImageLSB(Bitmap stegoImage) {
        int width = stegoImage.getWidth(), height = stegoImage.getHeight();
        int[] pixels = new int[width * height];
        stegoImage.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] raw = new byte[(int) getImageCapacity(stegoImage)];
        int byteIdx = 0, bitIdx = 0, currentByte = 0;

        for (int p : pixels) {
            int[] channels = {(p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF};
            for (int c : channels) {
                if (byteIdx < raw.length) {
                    currentByte |= ((c & 0x0F) << bitIdx);
                    bitIdx += 4;
                    if (bitIdx == 8) {
                        raw[byteIdx++] = (byte) currentByte;
                        bitIdx = 0; currentByte = 0;
                    }
                }
            }
            if (byteIdx >= raw.length) break;
        }
        return parseHeader(raw);
    }

    // --- Audio Steganography Logic ---
    // Hides secret data in the last 4 bits of each audio byte
    public static byte[] encodeAudioLSB(byte[] audioData, byte[] payload, String fileName) throws Exception {
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        int totalRequired = MAGIC.length + 4 + nameBytes.length + 4 + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalRequired);
        buffer.put(MAGIC);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(payload.length);
        buffer.put(payload);
        byte[] combined = buffer.array();

        int audioIdx = 44; // Skip the WAV header
        for (int i = 0; i < combined.length && audioIdx + 1 < audioData.length; i++) {
            byte p = combined[i];
            audioData[audioIdx] = (byte) ((audioData[audioIdx] & 0xF0) | (p & 0x0F));
            audioIdx++;
            audioData[audioIdx] = (byte) ((audioData[audioIdx] & 0xF0) | ((p >> 4) & 0x0F));
            audioIdx++;
        }
        return audioData;
    }

    // Extracts hidden bytes from audio data
    public static byte[] decodeAudioLSB(byte[] audioData) {
        byte[] raw = new byte[(int) getAudioCapacity(audioData)];
        int audioIdx = 44;
        for (int i = 0; i < raw.length && audioIdx + 1 < audioData.length; i++) {
            int low = audioData[audioIdx] & 0x0F;
            audioIdx++;
            int high = audioData[audioIdx] & 0x0F;
            audioIdx++;
            raw[i] = (byte) (low | (high << 4));
        }
        return parseHeader(raw);
    }

    // Parses the extracted raw bytes to separate the filename and data
    private static byte[] parseHeader(byte[] raw) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(raw);
            byte[] check = new byte[MAGIC.length];
            bb.get(check);
            if (!Arrays.equals(check, MAGIC)) return null;
            int nameLen = bb.getInt();
            byte[] nB = new byte[nameLen];
            bb.get(nB);
            int dataLen = bb.getInt();
            byte[] dB = new byte[dataLen];
            bb.get(dB);
            byte[] res = new byte[1 + nameLen + dataLen];
            res[0] = (byte) (nameLen & 0xFF);
            System.arraycopy(nB, 0, res, 1, nameLen);
            System.arraycopy(dB, 0, res, 1 + nameLen, dataLen);
            return res;
        } catch (Exception e) { return null; }
    }
}
