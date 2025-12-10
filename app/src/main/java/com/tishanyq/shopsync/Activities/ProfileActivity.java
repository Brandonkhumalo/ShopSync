package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Database.SyncManager;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.R;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private static final String WHATSAPP_NUMBER = "+263787700149";
    private static final String PREFS_NAME = "ShopSyncPrefs";
    
    private TextView tvShopId, tvShopName, tvOwner, tvPhone, tvServices, tvAddress, tvSyncStatus;
    private TextView tvProductKey, tvLicenseExpiry, tvDaysRemaining;
    private EditText etNewProductKey;
    private MaterialButton btnActivateKey, btnWhatsapp;
    private MaterialCardView cardRenewLicense, cardWhatsappSupport;
    private ImageView btnSettings;
    private DatabaseHelper db;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadProfile();
        loadLicenseInfo();
        setupClickListeners();
        checkLicenseExpiry();
    }

    private void initViews() {
        tvShopId = findViewById(R.id.tv_shop_id);
        tvShopName = findViewById(R.id.tv_shop_name);
        tvOwner = findViewById(R.id.tv_owner);
        tvPhone = findViewById(R.id.tv_phone);
        tvServices = findViewById(R.id.tv_services);
        tvAddress = findViewById(R.id.tv_address);
        tvSyncStatus = findViewById(R.id.tv_sync_status);
        
        tvProductKey = findViewById(R.id.tv_product_key);
        tvLicenseExpiry = findViewById(R.id.tv_license_expiry);
        tvDaysRemaining = findViewById(R.id.tv_days_remaining);
        etNewProductKey = findViewById(R.id.et_new_product_key);
        btnActivateKey = findViewById(R.id.btn_activate_key);
        btnWhatsapp = findViewById(R.id.btn_whatsapp);
        cardRenewLicense = findViewById(R.id.card_renew_license);
        cardWhatsappSupport = findViewById(R.id.card_whatsapp_support);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void loadProfile() {
        Shop shop = db.getShop();
        if (shop != null) {
            tvShopId.setText(shop.getId() != null ? shop.getId() : "Pending Sync...");
            tvShopName.setText(shop.getName());
            tvOwner.setText(shop.getOwnerName() + " " + shop.getOwnerSurname());
            tvPhone.setText(shop.getPhoneNumber());
            tvServices.setText(shop.getServices());
            tvAddress.setText(shop.getAddress());
            tvSyncStatus.setText(shop.isSynced() ? "Synced" : "Pending Sync");
        }
    }

    private void loadLicenseInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long expiresAt = prefs.getLong("expires_at", 0);
        String productKey = prefs.getString("product_key", null);
        
        if (productKey != null && !productKey.isEmpty()) {
            String maskedKey = maskProductKey(productKey);
            tvProductKey.setText(maskedKey);
        } else {
            tvProductKey.setText("Not activated");
        }
        
        if (expiresAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String expiryDate = sdf.format(new Date(expiresAt));
            tvLicenseExpiry.setText(expiryDate);
            
            long now = System.currentTimeMillis();
            if (expiresAt > now) {
                long daysRemaining = (expiresAt - now) / (1000 * 60 * 60 * 24);
                tvDaysRemaining.setText(daysRemaining + " days");
                
                if (daysRemaining <= 5) {
                    tvDaysRemaining.setBackgroundResource(R.drawable.badge_bg);
                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.warning_orange, null));
                } else {
                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.success_green, null));
                }
            } else {
                tvDaysRemaining.setText("EXPIRED");
                tvDaysRemaining.setTextColor(getResources().getColor(R.color.error_red, null));
            }
        } else {
            tvLicenseExpiry.setText("Not activated");
            tvDaysRemaining.setText("N/A");
        }
        
        executor.execute(() -> {
            String shopId = db.getStoredShopId();
            String appId = db.getAppId();
            
            if (shopId != null && appId != null) {
                try {
                    SyncManager syncManager = new SyncManager(this, db);
                    String response = syncManager.fetchLicenseInfo(shopId, appId);
                    
                    if (response != null) {
                        JSONObject json = new JSONObject(response);
                        String maskedKey = json.optString("product_key_masked", null);
                        long serverExpiresAt = json.optLong("expires_at", 0);
                        int daysRemaining = json.optInt("days_remaining", -1);
                        boolean expired = json.optBoolean("expired", false);
                        
                        mainHandler.post(() -> {
                            if (maskedKey != null && !maskedKey.isEmpty()) {
                                tvProductKey.setText(maskedKey);
                            }
                            
                            if (serverExpiresAt > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                tvLicenseExpiry.setText(sdf.format(new Date(serverExpiresAt)));
                                
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putLong("expires_at", serverExpiresAt);
                                editor.apply();
                            }
                            
                            if (expired) {
                                tvDaysRemaining.setText("EXPIRED");
                                tvDaysRemaining.setTextColor(getResources().getColor(R.color.error_red, null));
                            } else if (daysRemaining >= 0) {
                                tvDaysRemaining.setText(daysRemaining + " days");
                                if (daysRemaining <= 5) {
                                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.warning_orange, null));
                                } else {
                                    tvDaysRemaining.setTextColor(getResources().getColor(R.color.success_green, null));
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String maskProductKey(String productKey) {
        if (productKey == null || productKey.isEmpty()) {
            return "Not activated";
        }
        String[] parts = productKey.split("-");
        if (parts.length == 4) {
            return "****-****-****-" + parts[3];
        }
        if (productKey.length() >= 4) {
            return "****-****-****-" + productKey.substring(productKey.length() - 4);
        }
        return "****";
    }

    private void setupClickListeners() {
        btnActivateKey.setOnClickListener(v -> activateProductKey());
        
        btnWhatsapp.setOnClickListener(v -> openWhatsApp());
        
        cardWhatsappSupport.setOnClickListener(v -> openWhatsApp());
        
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void activateProductKey() {
        String productKey = etNewProductKey.getText().toString().trim().toUpperCase();
        
        if (productKey.isEmpty()) {
            Toast.makeText(this, "Please enter a product key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!productKey.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")) {
            Toast.makeText(this, "Invalid product key format. Use XXXX-XXXX-XXXX-XXXX", Toast.LENGTH_LONG).show();
            return;
        }
        
        btnActivateKey.setEnabled(false);
        btnActivateKey.setText("Activating...");
        
        executor.execute(() -> {
            String shopId = db.getStoredShopId();
            String appId = db.getAppId();
            
            if (shopId == null || appId == null) {
                mainHandler.post(() -> {
                    btnActivateKey.setEnabled(true);
                    btnActivateKey.setText("Activate Product Key");
                    Toast.makeText(this, "Shop not registered. Please register first.", Toast.LENGTH_LONG).show();
                });
                return;
            }
            
            try {
                SyncManager syncManager = new SyncManager(this, db);
                String response = syncManager.renewLicense(shopId, appId, productKey);
                
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    
                    if (json.has("error")) {
                        String error = json.getString("error");
                        mainHandler.post(() -> {
                            btnActivateKey.setEnabled(true);
                            btnActivateKey.setText("Activate Product Key");
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        });
                    } else {
                        long activatedAt = json.optLong("activated_at", System.currentTimeMillis());
                        long expiresAt = json.optLong("expires_at", 0);
                        int deviceSlot = json.optInt("device_slot", 1);
                        
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("activated_at", activatedAt);
                        editor.putLong("expires_at", expiresAt);
                        editor.putString("product_key", productKey);
                        editor.putBoolean("is_activated", true);
                        editor.apply();
                        
                        db.saveAppAuthorization(appId, shopId, deviceSlot, activatedAt, expiresAt);
                        
                        mainHandler.post(() -> {
                            btnActivateKey.setEnabled(true);
                            btnActivateKey.setText("Activate Product Key");
                            etNewProductKey.setText("");
                            Toast.makeText(this, "License activated successfully!", Toast.LENGTH_LONG).show();
                            loadLicenseInfo();
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        btnActivateKey.setEnabled(true);
                        btnActivateKey.setText("Activate Product Key");
                        Toast.makeText(this, "Failed to connect to server. Please check your internet connection.", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnActivateKey.setEnabled(true);
                    btnActivateKey.setText("Activate Product Key");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openWhatsApp() {
        try {
            String message = "Hello, I would like to purchase a ShopSync product key.";
            String url = "https://wa.me/" + WHATSAPP_NUMBER.replace("+", "") + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:" + WHATSAPP_NUMBER));
            smsIntent.putExtra("sms_body", "Hello, I would like to purchase a ShopSync product key.");
            startActivity(smsIntent);
        }
    }

    private void checkLicenseExpiry() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long expiresAt = prefs.getLong("expires_at", 0);
        
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            new AlertDialog.Builder(this)
                .setTitle("License Expired")
                .setMessage("Your product key has expired. Please enter a new product key to continue using ShopSync.\n\nContact us on WhatsApp: " + WHATSAPP_NUMBER)
                .setPositiveButton("Renew Now", (dialog, which) -> {
                    etNewProductKey.requestFocus();
                })
                .setNeutralButton("Contact Support", (dialog, which) -> {
                    openWhatsApp();
                })
                .setNegativeButton("Logout", (dialog, which) -> {
                    logoutUser();
                })
                .setCancelable(false)
                .show();
        }
    }

    private void logoutUser() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_activated", false);
        editor.apply();
        
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
