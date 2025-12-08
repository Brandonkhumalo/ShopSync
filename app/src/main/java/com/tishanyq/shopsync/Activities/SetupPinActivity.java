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

public class SetupPinActivity extends AppCompatActivity {
    private EditText etPin1, etPin2, etPin3, etPin4;
    private EditText etConfirmPin1, etConfirmPin2, etConfirmPin3, etConfirmPin4;
    private Button btnSetPin;
    private TextView tvShopName, tvError;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_pin);

        db = new DatabaseHelper(this);

        etPin1 = findViewById(R.id.et_pin_1);
        etPin2 = findViewById(R.id.et_pin_2);
        etPin3 = findViewById(R.id.et_pin_3);
        etPin4 = findViewById(R.id.et_pin_4);
        etConfirmPin1 = findViewById(R.id.et_confirm_pin_1);
        etConfirmPin2 = findViewById(R.id.et_confirm_pin_2);
        etConfirmPin3 = findViewById(R.id.et_confirm_pin_3);
        etConfirmPin4 = findViewById(R.id.et_confirm_pin_4);
        btnSetPin = findViewById(R.id.btn_set_pin);
        tvShopName = findViewById(R.id.tv_shop_name);
        tvError = findViewById(R.id.tv_error);

        Shop shop = db.getShop();
        if (shop == null) {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }
        tvShopName.setText(shop.getName());

        setupPinInputs();

        btnSetPin.setOnClickListener(v -> setPin());
    }

    private void setupPinInputs() {
        etPin1.addTextChangedListener(new PinTextWatcher(etPin1, null, etPin2));
        etPin2.addTextChangedListener(new PinTextWatcher(etPin2, etPin1, etPin3));
        etPin3.addTextChangedListener(new PinTextWatcher(etPin3, etPin2, etPin4));
        etPin4.addTextChangedListener(new PinTextWatcher(etPin4, etPin3, etConfirmPin1));

        etConfirmPin1.addTextChangedListener(new PinTextWatcher(etConfirmPin1, etPin4, etConfirmPin2));
        etConfirmPin2.addTextChangedListener(new PinTextWatcher(etConfirmPin2, etConfirmPin1, etConfirmPin3));
        etConfirmPin3.addTextChangedListener(new PinTextWatcher(etConfirmPin3, etConfirmPin2, etConfirmPin4));
        etConfirmPin4.addTextChangedListener(new PinTextWatcher(etConfirmPin4, etConfirmPin3, null));
    }

    private void setPin() {
        String pin = etPin1.getText().toString() +
                     etPin2.getText().toString() +
                     etPin3.getText().toString() +
                     etPin4.getText().toString();

        String confirmPin = etConfirmPin1.getText().toString() +
                            etConfirmPin2.getText().toString() +
                            etConfirmPin3.getText().toString() +
                            etConfirmPin4.getText().toString();

        if (pin.length() != 4) {
            tvError.setText("Please enter a 4-digit PIN");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (!pin.equals(confirmPin)) {
            tvError.setText("PINs do not match. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            clearConfirmPins();
            return;
        }

        if (db.updateShopPin(pin)) {
            tvError.setVisibility(View.GONE);
            Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else {
            tvError.setText("Failed to set PIN. Please try again.");
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void clearConfirmPins() {
        etConfirmPin1.setText("");
        etConfirmPin2.setText("");
        etConfirmPin3.setText("");
        etConfirmPin4.setText("");
        etConfirmPin1.requestFocus();
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
