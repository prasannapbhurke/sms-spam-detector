package com.example.spamdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 101;
    
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<MessageModel> messageList;
    private TextView spamCountText, safeCountText;
    private int spamCount = 0, safeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        spamCountText = findViewById(R.id.spam_count);
        safeCountText = findViewById(R.id.safe_count);
        recyclerView = findViewById(R.id.recycler_view);
        
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Add some sample data for preview
        addMessage("Win a free iPhone now! Click here", "Spam", "0.98");
        addMessage("Hey, are we still meeting for lunch?", "Not Spam", "0.12");

        checkPermissions();
    }

    public void addMessage(String text, String label, String confidence) {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        messageList.add(0, new MessageModel(text, label, confidence, currentTime));
        adapter.notifyItemInserted(0);
        recyclerView.scrollToPosition(0);

        if (label.equalsIgnoreCase("Spam")) {
            spamCount++;
            spamCountText.setText(String.valueOf(spamCount));
        } else {
            safeCount++;
            safeCountText.setText(String.valueOf(safeCount));
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS};
            } else {
                permissions = new String[]{Manifest.permission.RECEIVE_SMS};
            }
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Guard Active", Toast.LENGTH_SHORT).show();
        }
    }
}
