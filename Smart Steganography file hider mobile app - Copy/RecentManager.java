package com.example.smartstego;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Yeh class "Recent Activity" ke logs ko manage karti hai.
 * Iska maqsad yeh hai ke user ne haal hi mein jo bhi files hide ya extract ki hain, unka record rakha jaye.
 */
public class RecentManager {
    // Shared Preferences ka naam jisme recent logs save honge
    private static final String PREF_NAME = "SmartStego_Recent_Activity";
    private static final String KEY_RECENT = "recent_json_data";
    private static final int MAX_ITEMS = 20; // Zyada se zyada 20 items list mein rakhenge

    // Naya item list mein add karne ke liye function
    public static synchronized void addRecentItem(Context context, String fileName, String operation, String fileUri) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray(prefs.getString(KEY_RECENT, "[]"));

            // Naya JSON object banaya jisme file ki details hain
            JSONObject obj = new JSONObject();
            obj.put("name", fileName);
            obj.put("operation", operation); // "Embedded" ya "Extracted"
            // Current time aur date ko format kar ke save kar rahe hain
            obj.put("date", new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date()));
            obj.put("uri", fileUri);

            // Nayi entry ko hamesha pehle number par dikhane ke liye
            JSONArray newArray = new JSONArray();
            newArray.put(obj);
            
            // Purani entries ko limit ke mutabiq add karna
            for (int i = 0; i < array.length() && i < MAX_ITEMS - 1; i++) {
                newArray.put(array.get(i));
            }

            // Update ki hui list ko wapis save kar dena
            prefs.edit().putString(KEY_RECENT, newArray.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Saari saved items ko list ki surat mein wapis lena UI ke liye
    public static List<RecentItem> getRecentItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<RecentItem> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_RECENT, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new RecentItem(
                    obj.getString("name"),
                    obj.getString("operation"),
                    obj.getString("date"),
                    obj.getString("uri")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // List se kisi aik item ko delete karne ke liye
    public static void deleteRecentItem(Context context, int position) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray(prefs.getString(KEY_RECENT, "[]"));
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                if (i != position) newArray.put(array.get(i));
            }
            prefs.edit().putString(KEY_RECENT, newArray.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Poore activity log ko aik sath saaf (Clear) karne ke liye
    public static void clearRecent(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
