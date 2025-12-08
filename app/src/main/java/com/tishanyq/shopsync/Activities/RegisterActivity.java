package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.google.android.material.textfield.TextInputEditText;
import com.tishanyq.shopsync.R;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etSurname, etPhone, etShopName, etServices, etAddress;
    private Button btnRegister;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);

        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etPhone = findViewById(R.id.et_phone);
        etShopName = findViewById(R.id.et_shop_name);
        etServices = findViewById(R.id.et_services);
        etAddress = findViewById(R.id.et_address);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> registerShop());
    }

    private void registerShop() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String shopName = etShopName.getText().toString().trim();
        String services = etServices.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || surname.isEmpty() || phone.isEmpty() ||
                shopName.isEmpty() || services.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Shop shop = new Shop(shopName, name, surname, phone, services, address);
        long result = db.saveShop(shop);

        if (result != -1) {
            Toast.makeText(this, "Shop registered! Waiting for sync...", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }
}