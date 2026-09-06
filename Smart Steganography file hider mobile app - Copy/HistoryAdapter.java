package com.example.smartstego;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Yeh adapter "My Encoded Files" screen ki list dikhane ke liye use hota hai.
 * Humne har file ke sath Extract, Share aur Delete ka option diya hai.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;
    private Context context;

    public HistoryAdapter(List<HistoryItem> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // XML layout (item_history_file) ko attach kar rahe hain har row ke liye
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        
        // File ki details card par set karna (Naam, Type aur Date)
        holder.txtFileName.setText(item.getFileName());
        holder.txtFileType.setText(item.getFileType());
        holder.txtDate.setText(item.getDate());

        // Image hai ya Audio, us hisab se icon change karna taakay list achi lage
        if ("Audio".equalsIgnoreCase(item.getFileType())) {
            holder.imgFileType.setImageResource(android.R.drawable.ic_btn_speak_now);
        } else {
            holder.imgFileType.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Card par click hone par file ko Gallery mein open karne ki koshish karna
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(item.getFileUri());
                String mimeType = "Audio".equalsIgnoreCase(item.getFileType()) ? "audio/*" : "image/*";
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "File kholne mein masla hai, shayad delete ho gayi hai.", Toast.LENGTH_SHORT).show();
            }
        });

        // Extract button: Agar ghalti se gallery se file delete ho jaye to internal vault se nikalna
        holder.btnExtract.setOnClickListener(v -> {
            if (item.getInternalSecretPath() == null) {
                Toast.makeText(context, "Is file ki koi internal copy nahi mili.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Password mangne ke liye dialog box dikhana
            EditText etPass = new EditText(context);
            etPass.setHint("PIN likhein");
            new AlertDialog.Builder(context)
                    .setTitle("Internal Recovery")
                    .setMessage("App ke private vault se data wapis nikalein.")
                    .setView(etPass)
                    .setPositiveButton("Extract", (dialog, which) -> {
                        String pass = etPass.getText().toString().trim();
                        if (pass.isEmpty()) return;
                        // Vault se data nikalne ka process call karna
                        performInternalExtraction(item, pass);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Share button: Stego-file ko WhatsApp ya Email par bhejne ke liye
        holder.btnShare.setOnClickListener(v -> {
            try {
                Uri uri = Uri.parse(item.getFileUri());
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("Audio".equalsIgnoreCase(item.getFileType()) ? "audio/*" : "image/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(intent, "File kahan bhejna chahte hain?"));
            } catch (Exception e) {
                Toast.makeText(context, "Sharing fail ho gayi", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete button: Record aur internal copy dono hamesha ke liye khatam karna
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete")
                    .setMessage("Kya aap is file aur iski safety copy ko hamesha ke liye khatam karna chahte hain?")
                    .setPositiveButton("Haan", (dialog, which) -> {
                        // Manager ke zariye delete karna
                        HistoryManager.deleteHistoryItem(context, holder.getAdapterPosition());
                        historyList.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                        
                        // Agar list khali ho jaye to screen refresh karna
                        if (historyList.isEmpty() && context instanceof HistoryActivity) {
                            ((HistoryActivity) context).recreate();
                        }
                    })
                    .setNegativeButton("Nahi", null)
                    .show();
        });
    }

    // Internal vault se data nikalne ka asli logic
    private void performInternalExtraction(HistoryItem item, String pass) {
        try {
            byte[] encryptedData = FileUtils.readInternalSecret(item.getInternalSecretPath());
            if (encryptedData == null) throw new Exception("Copy nahi mili.");

            // Data ko unlock aur uncompress karna
            byte[] decryptedCompressed = CryptoStego.decryptBytes(encryptedData, pass);
            byte[] finalData = CryptoStego.decompress(decryptedCompressed);

            // File ko Downloads mein "Restored" ke naam se save karna
            Uri savedUri = FileUtils.saveRecoveredFile(context, finalData, "Restored_" + item.getFileName());

            if (savedUri != null) {
                Toast.makeText(context, "Success! File mil gayi aur open ho rahi hai.", Toast.LENGTH_SHORT).show();
                // File ko foran system viewer mein open karna
                FileUtils.openFile(context, savedUri, item.getFileName());
            }
        } catch (Exception e) {
            Toast.makeText(context, "Extraction fail! Password ghalat ho sakta hai.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    // ViewHolder class jo UI design elements ko Java se jorti hai
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFileType;
        TextView txtFileName, txtFileType, txtDate;
        ImageButton btnShare, btnDelete, btnExtract;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFileType = itemView.findViewById(R.id.imgFileType);
            txtFileName = itemView.findViewById(R.id.txtHistoryFileName);
            txtFileType = itemView.findViewById(R.id.txtHistoryFileType);
            txtDate = itemView.findViewById(R.id.txtHistoryDate);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnExtract = itemView.findViewById(R.id.btnExtract);
        }
    }
}
