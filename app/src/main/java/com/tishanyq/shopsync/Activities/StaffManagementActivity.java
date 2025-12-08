package com.tishanyq.shopsync.Activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.tishanyq.shopsync.R;

public class StaffManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

        TextView tvPlaceholder = findViewById(R.id.tv_placeholder);
        tvPlaceholder.setText("Staff Management\n\nComing Soon...");
    }
}