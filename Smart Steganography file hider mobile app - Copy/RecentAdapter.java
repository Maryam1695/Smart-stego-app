package com.example.smartstego;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Yeh adapter class "Recent Activity" ki list ko manage karti hai.
 * Har ek row mein data kaise dikhega aur buttons kaise kaam karenge, wo yahan likha hai.
 */
public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {

    private List<RecentItem> recentList;
    private Context context;

    public RecentAdapter(List<RecentItem> recentList, Context context) {
        this.recentList = recentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_recent layout ko inflate kar rahe hain har row ke liye
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentItem item = recentList.get(position);
        
        // Data ko UI elements mein set karna
        holder.txtName.setText(item.getFileName());
        holder.txtOp.setText(item.getOperation());
        holder.txtDate.setText(item.getDate());

        // Agar file extract hui hai to search icon dikhao, warna save icon
        if ("Extracted".equalsIgnoreCase(item.getOperation())) {
            holder.imgType.setImageResource(android.R.drawable.ic_menu_search);
        } else {
            holder.imgType.setImageResource(android.R.drawable.ic_menu_save);
        }

        // View Button: File ko uske asli format (Photos/WPS) mein kholne ke liye
        holder.btnView.setOnClickListener(v -> {
            FileUtils.openFile(context, Uri.parse(item.getFileUri()), item.getFileName());
        });

        // Share Button: File ko doosri apps (WhatsApp/Email) par bhejne ke liye
        holder.btnShare.setOnClickListener(v -> {
            try {
                Uri uri = Uri.parse(item.getFileUri());
                Intent intent = new Intent(Intent.ACTION_SEND);
                
                // File ki extension se uska sahi type pata karna
                String extension = MimeTypeMap.getFileExtensionFromUrl(item.getFileName());
                if (extension.isEmpty() && item.getFileName().contains(".")) {
                    extension = item.getFileName().substring(item.getFileName().lastIndexOf(".") + 1);
                }
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                
                intent.setType(mimeType != null ? mimeType : "*/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(intent, "Share via"));
            } catch (Exception e) {
                Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete Button: Activity log se item ko remove karna
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Log")
                    .setMessage("Kya aap is item ko list se hatana chahte hain?")
                    .setPositiveButton("Hatao", (dialog, which) -> {
                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            RecentManager.deleteRecentItem(context, currentPos);
                            recentList.remove(currentPos);
                            notifyItemRemoved(currentPos);
                        }
                    })
                    .setNegativeButton("Rehne do", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return recentList.size();
    }

    // ViewHolder class jo XML elements ko hold karti hai
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgType;
        TextView txtName, txtOp, txtDate;
        ImageButton btnView, btnShare, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgType = itemView.findViewById(R.id.imgOpType);
            txtName = itemView.findViewById(R.id.txtRecentName);
            txtOp = itemView.findViewById(R.id.txtRecentOp);
            txtDate = itemView.findViewById(R.id.txtRecentDate);
            btnView = itemView.findViewById(R.id.btnViewRecent);
            btnShare = itemView.findViewById(R.id.btnShareRecent);
            btnDelete = itemView.findViewById(R.id.btnDeleteRecent);
        }
    }
}
