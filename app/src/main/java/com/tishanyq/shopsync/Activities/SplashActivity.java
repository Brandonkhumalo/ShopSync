package com.tishanyq.shopsync.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Database.SyncManager;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.R;

import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final String PREFS_NAME = "ShopSyncPrefs";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private DatabaseHelper db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        db = new DatabaseHelper(this);
        
        new Handler().postDelayed(this::checkAppState, 2000);
    }
    
    private void checkAppState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean termsAccepted = prefs.getBoolean(KEY_TERMS_ACCEPTED, false);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (!termsAccepted) {
            startActivity(new Intent(this, TermsActivity.class));
            finish();
            return;
        }
        
        Shop shop = db.getShop();
        
        if (shop == null) {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }
        
        if (shop.getPin() == null || shop.getPin().isEmpty()) {
            startActivity(new Intent(this, SetupPinActivity.class));
            finish();
            return;
        }
        
        if (isNetworkAvailable() && isFirstLaunch) {
            validateProductKey(shop);
        } else {
            proceedToLogin(shop);
        }
    }
    
    private void validateProductKey(Shop shop) {
        if (!db.isAppActivated()) {
            showInvalidKeyDialog("Your app is not activated. Please enter a valid product key.");
            return;
        }
        
        String productKey = db.getStoredProductKey();
        if (productKey == null || productKey.isEmpty()) {
            showInvalidKeyDialog("No product key found. Please enter a valid product key to continue.");
            return;
        }
        
        new Thread(() -> {
            try {
                SyncManager syncManager = new SyncManager(this);
                String response = syncManager.validateProductKey(shop.getId(), productKey);
                
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean valid = json.optBoolean("valid", false);
                        String status = json.optString("status", "");
                        
                        if (valid && "active".equals(status)) {
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
                            proceedToLogin(shop);
                        } else if ("expired".equals(status)) {
                            showInvalidKeyDialog("Your subscription has expired. Please renew with a new product key to continue using ShopSync.");
                        } else {
                            showInvalidKeyDialog("Your product key is invalid. Please contact support for a new key.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing validation response", e);
                        proceedToLogin(shop);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error validating product key", e);
                runOnUiThread(() -> {
                    proceedToLogin(shop);
                });
            }
        }).start();
    }
    
    private void showInvalidKeyDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle("Product Key Required")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Enter Key", (dialog, which) -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("needs_activation", true);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Exit", (dialog, which) -> {
                finishAffinity();
            })
            .show();
    }
    
    private void proceedToLogin(Shop shop) {
        Intent intent = new Intent(this, PinLoginActivity.class);
        if (db.isLicenseExpired()) {
            intent.putExtra("license_expired", true);
        }
        startActivity(intent);
        finish();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}
