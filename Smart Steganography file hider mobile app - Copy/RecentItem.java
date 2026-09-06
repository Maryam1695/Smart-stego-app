package com.example.smartstego;

/**
 * Yeh ek simple model class hai jo "Recent Activity" ke har item ka data store karti hai.
 * Isme file ka naam, kaam ki type (Hide ya Extract), date aur system path rakha jata hai.
 */
public class RecentItem {
    private String fileName;
    private String operation; // Batayega ke file "Embedded" hui hai ya "Extracted"
    private String date;      // Time aur date ka record
    private String fileUri;   // Phone ki storage mein file kahan padi hai

    public RecentItem(String fileName, String operation, String date, String fileUri) {
        this.fileName = fileName;
        this.operation = operation;
        this.date = date;
        this.fileUri = fileUri;
    }

    // Saare getter functions taakay adapter is data ko screen par dikha sakay
    public String getFileName() { return fileName; }
    public String getOperation() { return operation; }
    public String getDate() { return date; }
    public String getFileUri() { return fileUri; }
}
