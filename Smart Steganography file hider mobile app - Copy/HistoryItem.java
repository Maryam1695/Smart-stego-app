package com.example.smartstego;

/**
 * Yeh ek simple model class hai jo history ke ek single item ka data store karti hai.
 * Isme file ka naam, type, date aur phone mein uski location save hoti hai.
 */
public class HistoryItem {
    private String fileName;    // File ka original naam
    private String fileType;    // Image hai ya Audio
    private String date;        // Kis din file hide ki gayi
    private String fileUri;     // Gallery/Storage ka path
    private String internalSecretPath; // App ke private folder mein jo encrypted copy hai uska path

    // Basic constructor jab internal path na ho
    public HistoryItem(String fileName, String fileType, String date, String fileUri) {
        this(fileName, fileType, date, fileUri, null);
    }

    // Full constructor saare data ke sath
    public HistoryItem(String fileName, String fileType, String date, String fileUri, String internalSecretPath) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.date = date;
        this.fileUri = fileUri;
        this.internalSecretPath = internalSecretPath;
    }

    // Saare getter aur setter functions taakay baqi classes is data ko use kar saken
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getDate() { return date; }
    public String getFileUri() { return fileUri; }
    public String getInternalSecretPath() { return internalSecretPath; }
    public void setInternalSecretPath(String path) { this.internalSecretPath = path; }
}
