package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Sale;
import com.tishanyq.shopsync.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReportingActivity extends AppCompatActivity {
    private Spinner spinnerPeriod;
    private TextView tvTotalSalesUSD, tvTotalSalesZWG, tvTransactions, tvDetails;
    private DatabaseHelper db;
    private MaterialCardView debts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting);

        db = new DatabaseHelper(this);

        spinnerPeriod = findViewById(R.id.spinner_period);
        tvTotalSalesUSD = findViewById(R.id.tv_total_sales_usd);
        tvTotalSalesZWG = findViewById(R.id.tv_total_sales_zwg);
        tvTransactions = findViewById(R.id.tv_transactions);
        tvDetails = findViewById(R.id.tv_details);

        debts = findViewById(R.id.debts_page);

        setupPeriodSpinner();

        debts.setOnClickListener(v -> {
            Intent intent = new Intent(ReportingActivity.this, Debts.class);
            startActivity(intent);
        });
    }

    private void setupPeriodSpinner() {
        String[] periods = {"Today", "Last 7 Days", "Last 30 Days"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadReport(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadReport(0);
    }

    private void loadReport(int periodType) {
        Calendar calendar = Calendar.getInstance();
        long endDate = calendar.getTimeInMillis();
        long startDate;

        switch (periodType) {
            case 0: // Today
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                startDate = calendar.getTimeInMillis();
                break;
            case 1: // Last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                startDate = calendar.getTimeInMillis();
                break;
            case 2: // Last 30 days
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                startDate = calendar.getTimeInMillis();
                break;
            default:
                startDate = 0;
        }

        List<Sale> sales = db.getSalesByDateRange(startDate, endDate);
        displayReport(sales);
    }

    private void displayReport(List<Sale> sales) {
        double totalUSD = 0;
        double totalZWG = 0;
        Map<String, Integer> itemCount = new HashMap<>();

        for (Sale sale : sales) {
            totalUSD += sale.getTotalUSD();
            totalZWG += sale.getTotalZWG();

            String itemName = sale.getItemName();
            itemCount.put(itemName, itemCount.getOrDefault(itemName, 0) + sale.getQuantity());
        }

        tvTotalSalesUSD.setText(String.format("$%.2f", totalUSD));
        tvTotalSalesZWG.setText(String.format("ZWG %.2f", totalZWG));
        tvTransactions.setText(String.valueOf(sales.size()));

        StringBuilder details = new StringBuilder("Top Items:\n\n");
        itemCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> details.append(entry.getKey())
                        .append(": ").append(entry.getValue()).append(" sold\n"));

        tvDetails.setText(details.toString());
    }
}