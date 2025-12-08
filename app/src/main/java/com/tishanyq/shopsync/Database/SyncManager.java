package com.tishanyq.shopsync.Database;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class SyncManager {
    private Context context;
    private DatabaseHelper db;
    private static final String BACKEND_URL = ""; // Leave blank as requested

    public SyncManager(Context context) {
        this.context = context;
        this.db = new DatabaseHelper(context);
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

    public void syncData() {
        if (BACKEND_URL.isEmpty() || !isNetworkAvailable()) {
            return;
        }

        try {
            // Sync shop data first
            // Sync unsynced items, sales, etc.
            // This is a placeholder - actual implementation depends on backend API

            db.logSync(true);
        } catch (Exception e) {
            e.printStackTrace();
            db.logSync(false);
        }
    }

    public long getDaysSinceLastSync() {
        long lastSync = db.getLastSyncTime();
        if (lastSync == 0) return 0;
        return (System.currentTimeMillis() - lastSync) / (1000 * 60 * 60 * 24);
    }
}
