package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tishanyq.shopsync.R;

public class AppInfoActivity extends AppCompatActivity {

    private static final String WHATSAPP_NUMBER = "+263788539918";

    private ImageView btnBack;
    private MaterialCardView cardWhatsappSupport;
    private MaterialButton btnWhatsapp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        cardWhatsappSupport = findViewById(R.id.card_whatsapp_support);
        btnWhatsapp = findViewById(R.id.btn_whatsapp);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        View.OnClickListener whatsappClickListener = v -> openWhatsApp();
        
        cardWhatsappSupport.setOnClickListener(whatsappClickListener);
        btnWhatsapp.setOnClickListener(whatsappClickListener);
    }

    private void openWhatsApp() {
        try {
            String url = "https://wa.me/" + WHATSAPP_NUMBER.replace("+", "").replace(" ", "");
            url += "?text=" + Uri.encode("Hello, I need help with ShopSync app.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.whatsapp"));
                startActivity(intent);
            } catch (Exception ex) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"));
                startActivity(intent);
            }
        }
    }
}
