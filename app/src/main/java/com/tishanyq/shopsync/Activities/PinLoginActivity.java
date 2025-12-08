package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Shop;
import com.tishanyq.shopsync.R;

public class PinLoginActivity extends AppCompatActivity {
    private EditText etPin1, etPin2, etPin3, etPin4;
    private Button btnLogin;
    private TextView tvShopName, tvError;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_login);

        db = new DatabaseHelper(this);

        etPin1 = findViewById(R.id.et_pin_1);
        etPin2 = findViewById(R.id.et_pin_2);
        etPin3 = findViewById(R.id.et_pin_3);
        etPin4 = findViewById(R.id.et_pin_4);
        btnLogin = findViewById(R.id.btn_login);
        tvShopName = findViewById(R.id.tv_shop_name);
        tvError = findViewById(R.id.tv_error);

        Shop shop = db.getShop();
        if (shop != null) {
            tvShopName.setText(shop.getName());
        }

        setupPinInputs();

        btnLogin.setOnClickListener(v -> verifyPin());
    }

    private void setupPinInputs() {
        etPin1.addTextChangedListener(new PinTextWatcher(etPin1, null, etPin2));
        etPin2.addTextChangedListener(new PinTextWatcher(etPin2, etPin1, etPin3));
        etPin3.addTextChangedListener(new PinTextWatcher(etPin3, etPin2, etPin4));
        etPin4.addTextChangedListener(new PinTextWatcher(etPin4, etPin3, null));
    }

    private void verifyPin() {
        String pin = etPin1.getText().toString() +
                     etPin2.getText().toString() +
                     etPin3.getText().toString() +
                     etPin4.getText().toString();

        if (pin.length() != 4) {
            tvError.setText("Please enter your 4-digit PIN");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (db.verifyPin(pin)) {
            tvError.setVisibility(View.GONE);
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else {
            tvError.setText("Incorrect PIN. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            clearPinInputs();
        }
    }

    private void clearPinInputs() {
        etPin1.setText("");
        etPin2.setText("");
        etPin3.setText("");
        etPin4.setText("");
        etPin1.requestFocus();
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
            tvError.setVisibility(View.GONE);
        }
    }
}
