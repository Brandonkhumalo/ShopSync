package com.tishanyq.shopsync.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.Database.SyncManager;
import com.tishanyq.shopsync.R;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    private TextView tvWelcome, tvSyncStatus, tvLicenseStatus;
    private CardView cardSales, cardInventory, cardStaff, cardReporting, cardProfile;
    private DatabaseHelper db;
    private SyncManager syncManager;
    private ExecutorService executor;
    private Handler mainHandler;
    
    private static final String BACKEND_URL = "http://192.168.1.13:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadShopInfo();
        checkSyncStatus();
        checkLicenseStatus();
        setupClickListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvSyncStatus = findViewById(R.id.tv_sync_status);
        tvLicenseStatus = findViewById(R.id.tv_license_status);
        cardSales = findViewById(R.id.card_sales);
        cardInventory = findViewById(R.id.card_inventory);
        cardStaff = findViewById(R.id.card_staff);
        cardReporting = findViewById(R.id.card_reporting);
        cardProfile = findViewById(R.id.card_profile);
    }

    private void loadShopInfo() {
        Shop shop = db.getShop();
        if (shop != null) {
            tvWelcome.setText("Welcome, " + shop.getName() + "!");
        }
    }

    private void checkSyncStatus() {
        long daysSinceSync = syncManager.getDaysSinceLastSync();
        if (daysSinceSync >= 10) {
            tvSyncStatus.setText("Please sync now to avoid data loss");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (daysSinceSync >= 7) {
            tvSyncStatus.setText("Sync recommended (" + daysSinceSync + " days)");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvSyncStatus.setText("Synced " + daysSinceSync + " days ago");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    
    private void checkLicenseStatus() {
        if (tvLicenseStatus == null) return;
        
        if (db.isLicenseExpired()) {
            tvLicenseStatus.setText("License expired - Tap to renew");
            tvLicenseStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvLicenseStatus.setOnClickListener(v -> showRenewLicenseDialog());
            showRenewLicenseDialog();
        } else {
            long daysUntilExpiry = syncManager.getDaysUntilExpiry();
            if (daysUntilExpiry <= 5) {
                tvLicenseStatus.setText("License expires in " + daysUntilExpiry + " days");
                tvLicenseStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvLicenseStatus.setText("License: " + daysUntilExpiry + " days remaining");
                tvLicenseStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            
            int deviceSlot = db.getDeviceSlot();
            if (deviceSlot > 0) {
                tvLicenseStatus.append(" (Device " + deviceSlot + ")");
            }
        }
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    private void showRenewLicenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_renew_license, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        
        TextInputEditText etProductKey = dialogView.findViewById(R.id.et_product_key);
        Button btnRenew = dialogView.findViewById(R.id.btn_renew);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        TextView tvError = dialogView.findViewById(R.id.tv_error);
        
        etProductKey.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                
                String text = s.toString().replaceAll("-", "").toUpperCase();
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length() && i < 16; i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(text.charAt(i));
                }
                
                if (!s.toString().equals(formatted.toString())) {
                    s.replace(0, s.length(), formatted.toString());
                }
                
                isFormatting = false;
            }
        });
        
        btnRenew.setOnClickListener(v -> {
            String productKey = etProductKey.getText().toString().trim();
            
            if (productKey.isEmpty()) {
                tvError.setText("Please enter a product key");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            
            if (productKey.length() != 19) {
                tvError.setText("Invalid product key format");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            
            if (!isNetworkAvailable()) {
                tvError.setText("Internet connection required to renew license");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            
            tvError.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            btnRenew.setEnabled(false);
            etProductKey.setEnabled(false);
            
            renewLicense(productKey, dialog, progressBar, btnRenew, etProductKey, tvError);
        });
        
        dialog.show();
    }
    
    private void renewLicense(String productKey, AlertDialog dialog, ProgressBar progressBar,
                              Button btnRenew, TextInputEditText etProductKey, TextView tvError) {
        String appId = db.getAppId();
        String shopId = db.getStoredShopId();
        
        if (BACKEND_URL.isEmpty()) {
            mainHandler.postDelayed(() -> {
                long currentTime = System.currentTimeMillis();
                long expiresAt = currentTime + (30L * 24 * 60 * 60 * 1000);
                db.saveAppAuthorization(appId, shopId, db.getDeviceSlot(), currentTime, expiresAt);
                
                dialog.dismiss();
                checkLicenseStatus();
            }, 1500);
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("product_key", productKey);
                
                URL url = new URL(BACKEND_URL + "/api/shops/" + shopId + "/devices/" + appId + "/renew");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                }
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject responseJson = new JSONObject(response.toString());
                    long activatedAt = responseJson.getLong("activated_at");
                    long expiresAt = responseJson.getLong("expires_at");
                    int deviceSlot = responseJson.getInt("device_slot");
                    
                    db.saveAppAuthorization(appId, shopId, deviceSlot, activatedAt, expiresAt);
                    
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        checkLicenseStatus();
                    });
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject errorJson = new JSONObject(response.toString());
                    String errorMessage = errorJson.optString("error", "Renewal failed");
                    
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnRenew.setEnabled(true);
                        etProductKey.setEnabled(true);
                        tvError.setText(errorMessage);
                        tvError.setVisibility(View.VISIBLE);
                    });
                }
                
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRenew.setEnabled(true);
                    etProductKey.setEnabled(true);
                    tvError.setText("Renewal failed: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void setupClickListeners() {
        cardSales.setOnClickListener(v -> {
            if (db.isLicenseExpired()) {
                showRenewLicenseDialog();
            } else {
                startActivity(new Intent(this, SalesActivity.class));
            }
        });

        cardInventory.setOnClickListener(v -> {
            if (db.isLicenseExpired()) {
                showRenewLicenseDialog();
            } else {
                startActivity(new Intent(this, InventoryActivity.class));
            }
        });

        cardStaff.setOnClickListener(v -> {
            if (db.isLicenseExpired()) {
                showRenewLicenseDialog();
            } else {
                startActivity(new Intent(this, StaffManagementActivity.class));
            }
        });

        cardReporting.setOnClickListener(v -> {
            if (db.isLicenseExpired()) {
                showRenewLicenseDialog();
            } else {
                startActivity(new Intent(this, ReportingActivity.class));
            }
        });

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkLicenseStatus();
        checkSyncStatus();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
