package com.tishanyq.shopsync.Database;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncManager {
    private Context context;
    private DatabaseHelper db;
    private static final String BACKEND_URL = "";
    private ExecutorService executor;
    private Handler mainHandler;

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
        void onLicenseExpired();
    }

    public SyncManager(Context context) {
        this.context = context;
        this.db = new DatabaseHelper(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public boolean needsSync() {
        long lastSync = db.getLastSyncTime();
        long currentTime = System.currentTimeMillis();
        long daysSinceSync = (currentTime - lastSync) / (1000 * 60 * 60 * 24);
        return daysSinceSync >= 10;
    }
    
    public boolean isLicenseExpired() {
        return db.isLicenseExpired();
    }
    
    public String getAppId() {
        return db.getAppId();
    }
    
    public String getShopId() {
        return db.getStoredShopId();
    }
    
    public int getDeviceSlot() {
        return db.getDeviceSlot();
    }

    public void syncData() {
        syncData(null);
    }

    public void syncData(SyncCallback callback) {
        if (BACKEND_URL.isEmpty()) {
            db.logSync(true);
            if (callback != null) {
                mainHandler.post(() -> callback.onSuccess("Sync completed (offline mode)"));
            }
            return;
        }
        
        if (!isNetworkAvailable()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("No network connection"));
            }
            return;
        }
        
        String appId = db.getAppId();
        String shopId = db.getStoredShopId();
        
        if (appId == null || shopId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("App not registered"));
            }
            return;
        }
        
        if (db.isLicenseExpired()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onLicenseExpired());
            }
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject syncData = new JSONObject();
                syncData.put("app_id", appId);
                syncData.put("device_slot", db.getDeviceSlot());
                
                JSONArray items = new JSONArray();
                JSONArray sales = new JSONArray();
                JSONArray debts = new JSONArray();
                
                syncData.put("items", items);
                syncData.put("sales", sales);
                syncData.put("debts", debts);
                
                URL url = new URL(BACKEND_URL + "/api/shops/" + shopId + "/sync");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-App-Id", appId);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(syncData.toString().getBytes("UTF-8"));
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
                    
                    db.clearUnsyncedRecords();
                    db.logSync(true);
                    
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess("Sync completed successfully"));
                    }
                } else if (responseCode == 403) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject errorJson = new JSONObject(response.toString());
                    boolean expired = errorJson.optBoolean("expired", false);
                    
                    if (expired) {
                        db.clearAuthorization();
                        if (callback != null) {
                            mainHandler.post(() -> callback.onLicenseExpired());
                        }
                    } else {
                        String errorMessage = errorJson.optString("error", "Access denied");
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError(errorMessage));
                        }
                    }
                } else {
                    db.logSync(false);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("Sync failed with code: " + responseCode));
                    }
                }
                
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                db.logSync(false);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Sync failed: " + e.getMessage()));
                }
            }
        });
    }

    public long getDaysSinceLastSync() {
        long lastSync = db.getLastSyncTime();
        if (lastSync == 0) return 0;
        return (System.currentTimeMillis() - lastSync) / (1000 * 60 * 60 * 24);
    }
    
    public long getDaysUntilExpiry() {
        long expiresAt = db.getExpiresAt();
        if (expiresAt == 0) return 0;
        long remaining = expiresAt - System.currentTimeMillis();
        return remaining > 0 ? remaining / (1000 * 60 * 60 * 24) : 0;
    }
}
