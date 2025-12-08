package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Database.SyncManager;
import com.tishanyq.shopsync.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvLastSync;
    private TextView tvSyncStatusBadge;
    private TextView tvAppVersion;
    private MaterialButton btnSyncNow;
    private ProgressBar progressSync;
    private MaterialCardView cardAppInfo;
    private ImageView btnBack;

    private DatabaseHelper db;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = new DatabaseHelper(this);
        syncManager = new SyncManager(this, db);

        initViews();
        setupListeners();
        loadSyncStatus();
        loadAppVersion();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvLastSync = findViewById(R.id.tv_last_sync);
        tvSyncStatusBadge = findViewById(R.id.tv_sync_status_badge);
        tvAppVersion = findViewById(R.id.tv_app_version);
        btnSyncNow = findViewById(R.id.btn_sync_now);
        progressSync = findViewById(R.id.progress_sync);
        cardAppInfo = findViewById(R.id.card_app_info);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSyncNow.setOnClickListener(v -> performSync());

        cardAppInfo.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AppInfoActivity.class);
            startActivity(intent);
        });
    }

    private void loadSyncStatus() {
        long lastSyncTime = db.getLastSyncTime();
        
        if (lastSyncTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(lastSyncTime));
            tvLastSync.setText(formattedDate);
            
            long daysSinceSync = (System.currentTimeMillis() - lastSyncTime) / (1000 * 60 * 60 * 24);
            
            if (daysSinceSync < 1) {
                tvSyncStatusBadge.setText("Up to date");
                tvSyncStatusBadge.setBackgroundResource(R.drawable.badge_success_bg);
            } else if (daysSinceSync < 7) {
                tvSyncStatusBadge.setText(daysSinceSync + " days ago");
                tvSyncStatusBadge.setBackgroundResource(R.drawable.badge_bg_blue);
            } else {
                tvSyncStatusBadge.setText("Needs sync");
                tvSyncStatusBadge.setBackgroundResource(R.drawable.badge_bg_gray);
            }
        } else {
            tvLastSync.setText("Never synced");
            tvSyncStatusBadge.setText("Not synced");
            tvSyncStatusBadge.setBackgroundResource(R.drawable.badge_bg_gray);
        }
    }

    private void loadAppVersion() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText("v" + versionName);
        } catch (Exception e) {
            tvAppVersion.setText("v1.0.0");
        }
    }

    private void performSync() {
        if (!syncManager.isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please connect to sync.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSyncNow.setEnabled(false);
        btnSyncNow.setText("Syncing...");
        progressSync.setVisibility(View.VISIBLE);

        syncManager.syncData(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText("Start Sync");
                    progressSync.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Sync completed successfully!", Toast.LENGTH_SHORT).show();
                    loadSyncStatus();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText("Start Sync");
                    progressSync.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Sync failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLicenseExpired() {
                runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText("Start Sync");
                    progressSync.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "License expired. Please renew to sync.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSyncStatus();
    }
}
