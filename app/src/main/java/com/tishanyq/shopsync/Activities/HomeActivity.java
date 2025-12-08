package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.Database.SyncManager;
import com.tishanyq.shopsync.R;

public class HomeActivity extends AppCompatActivity {
    private TextView tvWelcome, tvSyncStatus;
    private CardView cardSales, cardInventory, cardStaff, cardReporting, cardProfile;
    private DatabaseHelper db;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);
        syncManager = new SyncManager(this);

        initViews();
        loadShopInfo();
        checkSyncStatus();
        setupClickListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvSyncStatus = findViewById(R.id.tv_sync_status);
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
            tvSyncStatus.setText("⚠️ Please sync now to avoid data loss");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (daysSinceSync >= 7) {
            tvSyncStatus.setText("⚠️ Sync recommended (" + daysSinceSync + " days)");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvSyncStatus.setText("✓ Synced " + daysSinceSync + " days ago");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void setupClickListeners() {
        cardSales.setOnClickListener(v ->
                startActivity(new Intent(this, SalesActivity.class)));

        cardInventory.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));

        cardStaff.setOnClickListener(v ->
                startActivity(new Intent(this, StaffManagementActivity.class)));

        cardReporting.setOnClickListener(v ->
                startActivity(new Intent(this, ReportingActivity.class)));

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}
