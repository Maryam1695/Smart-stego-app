package com.example.smartstego;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Yeh class "My Encoded Files" yaani history ko manage karti hai.
 * Humne isme database ki jagah SharedPreferences use ki hain taakay app light-weight rahay.
 */
public class HistoryManager {
    // History data ko save karne ke liye preferences ka naam
    private static final String PREF_NAME = "SmartStego_Master_Vault_Final";
    private static final String KEY_HISTORY = "history_json_data";

    // Nayi file hide karne ke baad usay history mein add karna
    public static synchronized void saveHistoryItem(Context context, HistoryItem item) {
        if (context == null || item == null) return;
        saveInternal(context, item, true);
    }

    // History save karne ka asal logic: JSON format use kar rahe hain
    private static void saveInternal(Context context, HistoryItem item, boolean syncBackup) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            
            // Check kar rahe hain ke kahin duplicate naam to nahi aa raha
            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("name").equals(item.getFileName())) return;
            }

            // Naya data object bana kar JSON array mein shamil karna
            JSONObject obj = new JSONObject();
            obj.put("name", item.getFileName());
            obj.put("type", item.getFileType());
            obj.put("date", item.getDate());
            obj.put("uri", item.getFileUri());
            obj.put("internalPath", item.getInternalSecretPath());

            // Nayi entry ko list mein sab se upar dikhane ke liye
            JSONArray newArray = new JSONArray();
            newArray.put(obj);
            for (int i = 0; i < array.length(); i++) newArray.put(array.get(i));
            
            String json = newArray.toString();
            prefs.edit().putString(KEY_HISTORY, json).commit();
            
            // Jab bhi history update hogi, backup file bhi khud-ba-khud update ho jayegi
            if (syncBackup) RecoveryManager.syncBackupWithHistory(context, json);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Saved history ko wapis list ki surat mein nikalna taakay UI par dikhayi ja sakay
    public static List<HistoryItem> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<HistoryItem> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new HistoryItem(
                    obj.getString("name"), 
                    obj.getString("type"), 
                    obj.getString("date"), 
                    obj.getString("uri"),
                    obj.optString("internalPath", null)
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Backup restore karne ke waqt purani history ko wapis app mein dalna
    public static void restoreHistoryFromJson(Context context, String json) {
        if (json == null || json.isEmpty()) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, json).commit();
    }

    // Saari history ko text (JSON) ki surat mein lena backup banane ke liye
    public static String getHistoryAsJson(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_HISTORY, "[]");
    }

    // Kisi specific file ka record history se delete karna
    public static void deleteHistoryItem(Context context, int position) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                if (i != position) newArray.put(array.get(i));
            }
            String finalJson = newArray.toString();
            prefs.edit().putString(KEY_HISTORY, finalJson).commit();
            
            // Delete hone par backup bhi update kar rahe hain
            RecoveryManager.syncBackupWithHistory(context, finalJson);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Poori history list ko clear karna
    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit();
        RecoveryManager.syncBackupWithHistory(context, "[]");
    }
}
