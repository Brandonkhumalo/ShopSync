package com.tishanyq.shopsync.Activities;

import android.os.Bundle;
import com.tishanyq.shopsync.R;
import android.content.Intent;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            DatabaseHelper db = new DatabaseHelper(this);
            Shop shop = db.getShop();

            Intent intent;
            if (shop == null) {
                intent = new Intent(SplashActivity.this, RegisterActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}