package com.tishanyq.shopsync.Activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.R;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvShopId, tvShopName, tvOwner, tvPhone, tvServices, tvAddress, tvSyncStatus;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);

        tvShopId = findViewById(R.id.tv_shop_id);
        tvShopName = findViewById(R.id.tv_shop_name);
        tvOwner = findViewById(R.id.tv_owner);
        tvPhone = findViewById(R.id.tv_phone);
        tvServices = findViewById(R.id.tv_services);
        tvAddress = findViewById(R.id.tv_address);
        tvSyncStatus = findViewById(R.id.tv_sync_status);

        loadProfile();
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
            tvSyncStatus.setText(shop.isSynced() ? "✓ Synced" : "⏳ Pending Sync");
        }
    }
}