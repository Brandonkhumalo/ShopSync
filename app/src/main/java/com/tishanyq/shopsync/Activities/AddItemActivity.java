package com.tishanyq.shopsync.Activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Item;
import com.google.android.material.textfield.TextInputEditText;
import com.tishanyq.shopsync.R;

public class AddItemActivity extends AppCompatActivity {
    private TextInputEditText etItemName, etPriceUSD, etPriceZWG, etQuantity;
    private Spinner spinnerCategory;
    private Button btnSave;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = new DatabaseHelper(this);

        etItemName = findViewById(R.id.et_item_name);
        etPriceUSD = findViewById(R.id.et_price_usd);
        etPriceZWG = findViewById(R.id.et_price_zwg);
        etQuantity = findViewById(R.id.et_quantity);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save);

        setupCategorySpinner();
        btnSave.setOnClickListener(v -> saveItem());
    }

    private void setupCategorySpinner() {
        String[] categories = {"Food & Beverages", "Electronics", "Clothing",
                "Hardware", "Stationery", "Gas", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void saveItem() {
        String name = etItemName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String priceUSDStr = etPriceUSD.getText().toString().trim();
        String priceZWGStr = etPriceZWG.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();

        if (name.isEmpty() || priceUSDStr.isEmpty() || priceZWGStr.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double priceUSD = Double.parseDouble(priceUSDStr);
            double priceZWG = Double.parseDouble(priceZWGStr);
            int quantity = Integer.parseInt(quantityStr);

            Item item = new Item(name, category, priceUSD, priceZWG, quantity);
            long result = db.addItem(item);

            if (result != -1) {
                Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}