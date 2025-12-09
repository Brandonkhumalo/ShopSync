package com.tishanyq.shopsync.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.google.android.material.textfield.TextInputEditText;
import com.tishanyq.shopsync.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etSurname, etPhone, etShopName, etServices, etAddress;
    private EditText etPin1, etPin2, etPin3, etPin4;
    private Button btnRegister;
    private DatabaseHelper db;
    private ExecutorService executor;
    private Handler mainHandler;
    
    private static final String BACKEND_URL = "https://shopsync-qx6o.onrender.com"; //"http://192.168.1.13:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etPhone = findViewById(R.id.et_phone);
        etShopName = findViewById(R.id.et_shop_name);
        etServices = findViewById(R.id.et_services);
        etAddress = findViewById(R.id.et_address);
        etPin1 = findViewById(R.id.et_pin_1);
        etPin2 = findViewById(R.id.et_pin_2);
        etPin3 = findViewById(R.id.et_pin_3);
        etPin4 = findViewById(R.id.et_pin_4);
        btnRegister = findViewById(R.id.btn_register);

        setupPinInputs();
        btnRegister.setOnClickListener(v -> registerShop());
    }

    private void setupPinInputs() {
        etPin1.addTextChangedListener(new PinTextWatcher(etPin1, null, etPin2));
        etPin2.addTextChangedListener(new PinTextWatcher(etPin2, etPin1, etPin3));
        etPin3.addTextChangedListener(new PinTextWatcher(etPin3, etPin2, etPin4));
        etPin4.addTextChangedListener(new PinTextWatcher(etPin4, etPin3, null));
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void registerShop() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String shopName = etShopName.getText().toString().trim();
        String services = etServices.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String pin = etPin1.getText().toString() +
                     etPin2.getText().toString() +
                     etPin3.getText().toString() +
                     etPin4.getText().toString();

        if (name.isEmpty() || surname.isEmpty() || phone.isEmpty() ||
                shopName.isEmpty() || services.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pin.length() != 4) {
            Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Internet connection required for first-time registration", Toast.LENGTH_LONG).show();
            return;
        }

        Shop shop = new Shop(shopName, name, surname, phone, services, address, pin);
        
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");
        
        if (BACKEND_URL.isEmpty()) {
            long result = db.saveShop(shop);
            if (result != -1) {
                String tempAppId = "APP_" + System.currentTimeMillis();
                String tempShopId = "SHOP_" + System.currentTimeMillis();
                db.savePendingRegistration(tempAppId, tempShopId, 1);
                showProductKeyDialog(tempAppId, tempShopId, 1);
            } else {
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("name", shopName);
                jsonBody.put("owner_name", name);
                jsonBody.put("owner_surname", surname);
                jsonBody.put("phone_number", phone);
                jsonBody.put("services", services);
                jsonBody.put("address", address);
                jsonBody.put("pin", pin);
                
                URL url = new URL(BACKEND_URL + "/api/shops");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                }
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 201) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject responseJson = new JSONObject(response.toString());
                    String shopId = responseJson.getString("shop_id");
                    String appId = responseJson.getString("app_id");
                    int deviceSlot = responseJson.getInt("device_slot");
                    
                    long result = db.saveShop(shop);
                    if (result != -1) {
                        db.savePendingRegistration(appId, shopId, deviceSlot);
                        db.updateShopId(shopId);
                    }
                    
                    mainHandler.post(() -> {
                        showProductKeyDialog(appId, shopId, deviceSlot);
                    });
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    mainHandler.post(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + response.toString(), Toast.LENGTH_LONG).show();
                    });
                }
                
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void showProductKeyDialog(String appId, String shopId, int deviceSlot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_key, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        
        TextInputEditText etProductKey = dialogView.findViewById(R.id.et_product_key);
        Button btnActivate = dialogView.findViewById(R.id.btn_activate);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        TextView tvError = dialogView.findViewById(R.id.tv_error);
        
        etProductKey.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private String previousText = "";
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                
                String text = s.toString().replaceAll("-", "").toUpperCase();
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length() && i < 16; i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(text.charAt(i));
                }
                
                if (!s.toString().equals(formatted.toString())) {
                    s.replace(0, s.length(), formatted.toString());
                }
                
                isFormatting = false;
            }
        });
        
        btnActivate.setOnClickListener(v -> {
            String productKey = etProductKey.getText().toString().trim();
            
            if (productKey.isEmpty()) {
                tvError.setText("Please enter a product key");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            
            if (productKey.length() != 19) {
                tvError.setText("Invalid product key format");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            
            tvError.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            btnActivate.setEnabled(false);
            etProductKey.setEnabled(false);
            
            activateProductKey(productKey, appId, shopId, deviceSlot, dialog, progressBar, btnActivate, etProductKey, tvError);
        });
        
        dialog.show();
    }
    
    private void activateProductKey(String productKey, String appId, String shopId, int deviceSlot,
                                    AlertDialog dialog, ProgressBar progressBar, Button btnActivate,
                                    TextInputEditText etProductKey, TextView tvError) {
        
        if (BACKEND_URL.isEmpty()) {
            mainHandler.postDelayed(() -> {
                long currentTime = System.currentTimeMillis();
                long expiresAt = currentTime + (30L * 24 * 60 * 60 * 1000);
                db.saveAppAuthorization(appId, shopId, deviceSlot, currentTime, expiresAt);
                
                dialog.dismiss();
                Toast.makeText(RegisterActivity.this, "App activated successfully!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                finish();
            }, 1500);
            return;
        }
        
        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("product_key", productKey);
                jsonBody.put("app_id", appId);
                
                URL url = new URL(BACKEND_URL + "/api/shops/" + shopId + "/product-keys/activate");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("UTF-8"));
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
                    
                    JSONObject responseJson = new JSONObject(response.toString());
                    long activatedAt = responseJson.getLong("activated_at");
                    long expiresAt = responseJson.getLong("expires_at");
                    
                    db.saveAppAuthorization(appId, shopId, deviceSlot, activatedAt, expiresAt);
                    
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "App activated successfully!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                        finish();
                    });
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject errorJson = new JSONObject(response.toString());
                    String errorMessage = errorJson.optString("error", "Activation failed");
                    
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnActivate.setEnabled(true);
                        etProductKey.setEnabled(true);
                        tvError.setText(errorMessage);
                        tvError.setVisibility(View.VISIBLE);
                    });
                }
                
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnActivate.setEnabled(true);
                    etProductKey.setEnabled(true);
                    tvError.setText("Activation failed: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private class PinTextWatcher implements TextWatcher {
        private EditText current;
        private EditText prev;
        private EditText next;

        PinTextWatcher(EditText current, EditText prev, EditText next) {
            this.current = current;
            this.prev = prev;
            this.next = next;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && next != null) {
                next.requestFocus();
            } else if (s.length() == 0 && prev != null) {
                prev.requestFocus();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
