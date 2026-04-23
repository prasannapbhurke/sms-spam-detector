package com.example.spamdetector;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MessageAdapter.FeedbackListener {
    private static final int PERMISSION_CODE = 101;

    private MessageAdapter adapter;
    private final List<MessageModel> messageList = new ArrayList<>();
    private TextView spamCountText;
    private TextView safeCountText;
    private TextView timelineSummaryText;
    private FloatingActionButton manualScanFab;
    private LocalPredictor localPredictor;
    private DrawerLayout drawerLayout;
    private SwitchMaterial navAutoScanSwitch;
    private TextView navAutoScanStatus;
    private Toolbar toolbar;
    private String activeFilter = "All";
    private final String[] scanScopes = {"All", "Unread only", "Last 24 hours", "Last 7 days"};
    private final String[] filterOptions = {"All", "Contacts", "Unknown"};

    private int spamCount = 0;
    private int safeCount = 0;
    private int flaggedCount = 0;
    private int totalCount = 0;
    private int correctedCount = 0;

    private final BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra(MessageStore.EXTRA_MESSAGE_KEY)) {
                handleRealtimeMessage(intent);
            } else {
                loadSavedMessages();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        spamCountText = findViewById(R.id.spam_count);
        safeCountText = findViewById(R.id.safe_count);
        timelineSummaryText = findViewById(R.id.timeline_summary);
        manualScanFab = findViewById(R.id.manual_scan_fab);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        adapter = new MessageAdapter(messageList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        attachSwipeActions(recyclerView);

        setupDrawer();
        updateToolbarSubtitle();

        new Thread(() -> {
            localPredictor = new LocalPredictor(MainActivity.this);
            runOnUiThread(this::updateToolbarSubtitle);
        }).start();

        manualScanFab.setOnClickListener(v -> {
            if (hasSmsReadPermission()) {
                scanInboxHistory();
            } else {
                checkPermissions();
            }
        });

        checkPermissions();
    }

    private void setupDrawer() {
        NavigationView navigationView = findViewById(R.id.navigation_view);
        View headerView = navigationView.getHeaderView(0);
        navAutoScanSwitch = headerView.findViewById(R.id.nav_auto_scan_switch);
        navAutoScanStatus = headerView.findViewById(R.id.nav_auto_scan_status);
        navAutoScanSwitch.setChecked(SettingsStore.isAutoScanEnabled(this));
        updateAutoScanState(navAutoScanSwitch.isChecked());
        navAutoScanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsStore.setAutoScanEnabled(MainActivity.this, isChecked);
            updateAutoScanState(isChecked);
            updateToolbarSubtitle();
        });
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_filters) {
                showFilterDialog();
            } else if (itemId == R.id.nav_scan_scope) {
                showScanScopeDialog();
            } else if (itemId == R.id.nav_scan_now) {
                if (hasSmsReadPermission()) {
                    scanInboxHistory();
                } else {
                    checkPermissions();
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void updateAutoScanState(boolean enabled) {
        if (navAutoScanStatus != null) {
            navAutoScanStatus.setText(enabled ? "Protection active" : "Protection paused");
        }
    }

    private void updateToolbarSubtitle() {
        if (toolbar == null) {
            return;
        }
        String filterLabel;
        if ("Spam".equals(activeFilter)) {
            filterLabel = "Unknown";
        } else if ("Safe".equals(activeFilter)) {
            filterLabel = "Contacts";
        } else {
            filterLabel = "All";
        }
        toolbar.setSubtitle(filterLabel + " • Scope: " + SettingsStore.getScanScope(this));
        toolbar.setSubtitleTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void showFilterDialog() {
        int selectedIndex = "Spam".equals(activeFilter) ? 2 : ("Safe".equals(activeFilter) ? 1 : 0);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Filters")
                .setSingleChoiceItems(filterOptions, selectedIndex, (dialog, which) -> {
                    if (which == 1) {
                        activeFilter = "Safe";
                    } else if (which == 2) {
                        activeFilter = "Spam";
                    } else {
                        activeFilter = "All";
                    }
                    updateToolbarSubtitle();
                    loadSavedMessages();
                    dialog.dismiss();
                })
                .show();
    }

    private void showScanScopeDialog() {
        String currentScope = SettingsStore.getScanScope(this);
        int selectedIndex = 0;
        for (int i = 0; i < scanScopes.length; i++) {
            if (scanScopes[i].equals(currentScope)) {
                selectedIndex = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan Scope")
                .setSingleChoiceItems(scanScopes, selectedIndex, (dialog, which) -> {
                    SettingsStore.setScanScope(this, scanScopes[which]);
                    updateToolbarSubtitle();
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MessageStore.ACTION_MESSAGES_UPDATED);
        ContextCompat.registerReceiver(this, updatesReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        loadSavedMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updatesReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void loadSavedMessages() {
        new Thread(() -> {
            List<MessageModel> saved = MessageStore.getMessages(MainActivity.this);
            spamCount = MessageStore.spamCount(MainActivity.this);
            safeCount = MessageStore.safeCount(MainActivity.this);
            totalCount = MessageStore.totalCount(MainActivity.this);
            correctedCount = MessageStore.feedbackCount(MainActivity.this);

            final List<MessageModel> filtered = new ArrayList<>();
            for (MessageModel messageModel : saved) {
                if (matchesFilter(messageModel)) {
                    filtered.add(messageModel);
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (adapter == null || spamCountText == null || safeCountText == null) {
                    return;
                }
                messageList.clear();
                messageList.addAll(filtered);
                adapter.refreshTimeline();
                spamCountText.setText(String.valueOf(spamCount));
                safeCountText.setText(String.valueOf(safeCount));
                updateTimelineSummary();
            });
        }).start();
    }

    private void handleRealtimeMessage(Intent intent) {
        MessageModel incoming = new MessageModel(
                intent.getStringExtra(MessageStore.EXTRA_MESSAGE_KEY),
                intent.getStringExtra(MessageStore.EXTRA_MESSAGE),
                intent.getStringExtra(MessageStore.EXTRA_LABEL),
                intent.getStringExtra(MessageStore.EXTRA_CONFIDENCE),
                intent.getStringExtra(MessageStore.EXTRA_TIME),
                intent.getStringExtra(MessageStore.EXTRA_SENDER),
                intent.getStringExtra(MessageStore.EXTRA_CATEGORY),
                intent.getStringExtra(MessageStore.EXTRA_REASONS),
                intent.getBooleanExtra(MessageStore.EXTRA_HAS_LINK, false)
        );

        if (incoming.getMessageKey() == null) {
            loadSavedMessages();
            return;
        }

        int existingIndex = findMessagePosition(incoming.getMessageKey());
        if (existingIndex >= 0) {
            messageList.remove(existingIndex);
        }

        totalCount++;
        if ("Spam".equalsIgnoreCase(incoming.getDisplayLabel())) {
            spamCount++;
        } else {
            safeCount++;
        }

        if (matchesFilter(incoming)) {
            messageList.add(0, incoming);
        }

        if (spamCountText != null) {
            spamCountText.setText(String.valueOf(spamCount));
        }
        if (safeCountText != null) {
            safeCountText.setText(String.valueOf(safeCount));
        }
        updateTimelineSummary();
        adapter.refreshTimeline();
    }

    private boolean matchesFilter(MessageModel model) {
        if ("All".equals(activeFilter)) {
            return true;
        }
        if ("Spam".equals(activeFilter)) {
            return model.getDisplayLabel().equalsIgnoreCase("Spam");
        }
        if ("Safe".equals(activeFilter)) {
            return !model.getDisplayLabel().equalsIgnoreCase("Spam");
        }
        return true;
    }

    private void updateTimelineSummary() {
        if (timelineSummaryText != null) {
            timelineSummaryText.setText(totalCount + " scanned | " + spamCount + " spam | " + correctedCount + " corrected by you");
        }
    }

    private void attachSwipeActions(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                MessageModel model = adapter.getMessageAtAdapterPosition(viewHolder.getBindingAdapterPosition());
                return model == null ? 0 : super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                MessageModel model = adapter.getMessageAtAdapterPosition(viewHolder.getBindingAdapterPosition());
                if (model == null) {
                    adapter.refreshTimeline();
                    return;
                }
                if (direction == ItemTouchHelper.LEFT) {
                    onMarkSpam(model);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    onMarkSafe(model);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                MessageModel model = adapter.getMessageAtAdapterPosition(viewHolder.getBindingAdapterPosition());
                if (model == null) {
                    return;
                }
                View itemView = viewHolder.itemView;
                int backgroundColor = dX > 0
                        ? ContextCompat.getColor(MainActivity.this, R.color.safe_green)
                        : ContextCompat.getColor(MainActivity.this, R.color.spam_red);
                Paint paint = new Paint();
                paint.setColor(backgroundColor);
                if (dX > 0) {
                    canvas.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), paint);
                } else {
                    canvas.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private boolean hasSmsReadPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_CODE);
        }
    }

    private void scanInboxHistory() {
        if (localPredictor == null) {
            Toast.makeText(this, "Wait for AI engine", Toast.LENGTH_SHORT).show();
            return;
        }
        if (toolbar != null) {
            toolbar.setSubtitle("Scanning inbox...");
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        manualScanFab.setEnabled(false);

        new Thread(() -> {
            flaggedCount = 0;
            int scannedCount = 0;
            int importedCount = 0;
            int skippedCount = 0;
            Cursor cursor = null;
            try {
                String scope = SettingsStore.getScanScope(this);
                long now = System.currentTimeMillis();
                String selection = null;
                String[] selectionArgs = null;

                if ("Unread only".equals(scope)) {
                    selection = Telephony.Sms.READ + " = ?";
                    selectionArgs = new String[]{"0"};
                } else if ("Last 24 hours".equals(scope)) {
                    selection = Telephony.Sms.DATE + " >= ?";
                    selectionArgs = new String[]{String.valueOf(now - 24L * 60 * 60 * 1000)};
                } else if ("Last 7 days".equals(scope)) {
                    selection = Telephony.Sms.DATE + " >= ?";
                    selectionArgs = new String[]{String.valueOf(now - 7L * 24 * 60 * 60 * 1000)};
                }

                cursor = getContentResolver().query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
                        selection,
                        selectionArgs,
                        Telephony.Sms.DEFAULT_SORT_ORDER
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String sender = cursor.getString(0);
                        String body = cursor.getString(1);
                        long date = cursor.getLong(2);
                        scannedCount++;

                        if (body == null || body.trim().isEmpty()) {
                            skippedCount++;
                            continue;
                        }

                        String learnedLabel = MessageStore.getLearnedLabel(this, body, sender);
                        float score = localPredictor.predict(body);
                        RiskAnalyzer.RiskResult risk = RiskAnalyzer.analyze(body, sender, 0, score);
                        if (learnedLabel != null) {
                            risk.label = learnedLabel;
                            risk.reasons = risk.reasons + " | Learned from your feedback";
                        }
                        String confidenceLabel = learnedLabel != null
                                ? "100%"
                                : String.format(java.util.Locale.getDefault(), "%.0f%%", (risk.label.equalsIgnoreCase("Spam") ? score : 1 - score) * 100f);

                        boolean inserted = MessageStore.saveMessage(
                                this,
                                body,
                                risk.label,
                                confidenceLabel,
                                sender,
                                DateFormat.getDateTimeInstance().format(new Date(date)),
                                risk.category,
                                risk.reasons,
                                risk.hasLink,
                                risk.language,
                                false
                        );

                        if (inserted) {
                            importedCount++;
                            if (risk.label.equalsIgnoreCase("Spam")) {
                                flaggedCount++;
                            }
                        } else {
                            skippedCount++;
                        }
                    }
                }

                sendBroadcast(new Intent(MessageStore.ACTION_MESSAGES_UPDATED));

                int finalScannedCount = scannedCount;
                int finalImportedCount = importedCount;
                int finalSkippedCount = skippedCount;
                int finalFlaggedCount = flaggedCount;
                runOnUiThread(() -> {
                    loadSavedMessages();
                    updateToolbarSubtitle();
                    Toast.makeText(
                            MainActivity.this,
                            "Scanned " + finalScannedCount + ", added " + finalImportedCount + ", skipped " + finalSkippedCount + ", flagged " + finalFlaggedCount,
                            Toast.LENGTH_LONG
                    ).show();
                    manualScanFab.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateToolbarSubtitle();
                    Toast.makeText(MainActivity.this, "Scan failed.", Toast.LENGTH_SHORT).show();
                    manualScanFab.setEnabled(true);
                });
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }).start();
    }

    @Override
    public void onMarkSpam(MessageModel model) {
        boolean updated = MessageStore.applyManualLabel(this, model, "Spam");
        if (updated) {
            applyImmediateFeedback(model, "Spam");
            correctedCount++;
            updateTimelineSummary();
        }
        Toast.makeText(this, updated ? "Learning saved: marked as Spam" : "Could not save learning", Toast.LENGTH_SHORT).show();
        updateToolbarSubtitle();
    }

    @Override
    public void onMarkSafe(MessageModel model) {
        boolean updated = MessageStore.applyManualLabel(this, model, "Safe");
        if (updated) {
            applyImmediateFeedback(model, "Safe");
            correctedCount++;
            updateTimelineSummary();
        }
        Toast.makeText(this, updated ? "Learning saved: marked as Safe" : "Could not save learning", Toast.LENGTH_SHORT).show();
        updateToolbarSubtitle();
    }

    private void applyImmediateFeedback(MessageModel originalModel, String newLabel) {
        if (adapter == null || originalModel == null) {
            return;
        }

        int position = findMessagePosition(originalModel.getMessageKey());
        if (position < 0 || position >= messageList.size()) {
            loadSavedMessages();
            return;
        }

        MessageModel updatedModel = originalModel.withManualLabel(newLabel);
        boolean wasSpam = originalModel.getLabel().equalsIgnoreCase("Spam");
        boolean nowSpam = newLabel.equalsIgnoreCase("Spam");

        if (wasSpam != nowSpam) {
            spamCount += nowSpam ? 1 : -1;
            safeCount += nowSpam ? -1 : 1;
            spamCount = Math.max(0, spamCount);
            safeCount = Math.max(0, safeCount);
            spamCountText.setText(String.valueOf(spamCount));
            safeCountText.setText(String.valueOf(safeCount));
        }

        if (matchesFilter(updatedModel)) {
            messageList.set(position, updatedModel);
        } else {
            messageList.remove(position);
        }
        adapter.refreshTimeline();
    }

    private int findMessagePosition(String messageKey) {
        if (messageKey == null) {
            return -1;
        }
        for (int i = 0; i < messageList.size(); i++) {
            MessageModel model = messageList.get(i);
            if (messageKey.equals(model.getMessageKey())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && hasSmsReadPermission()) {
            loadSavedMessages();
        }
    }
}
