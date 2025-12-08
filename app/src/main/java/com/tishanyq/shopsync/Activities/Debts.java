package com.tishanyq.shopsync.Activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Debt;
import com.tishanyq.shopsync.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Debts extends AppCompatActivity {
    private EditText etSearch;
    private Button btnFilterDate, btnClearFilter;
    private TextView tvTotalDebtUSD, tvTotalDebtZWG, tvActiveDebts;
    private RecyclerView recyclerView;
    private CheckBox cbShowCleared;
    private DatabaseHelper db;
    private DebtAdapter adapter;
    private long filterStartDate = 0, filterEndDate = 0;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debts);

        db = new DatabaseHelper(this);

        etSearch = findViewById(R.id.et_search);
        btnFilterDate = findViewById(R.id.btn_filter_date);
        btnClearFilter = findViewById(R.id.btn_clear_filter);
        tvTotalDebtUSD = findViewById(R.id.tv_total_debt_usd);
        tvTotalDebtZWG = findViewById(R.id.tv_total_debt_zwg);
        tvActiveDebts = findViewById(R.id.tv_active_debts);
        recyclerView = findViewById(R.id.recycler_view);
        cbShowCleared = findViewById(R.id.cb_show_cleared);

        setupRecyclerView();
        setupListeners();
        loadDebts();
        updateTotals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDebts();
        updateTotals();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DebtAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadDebts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cbShowCleared.setOnCheckedChangeListener((buttonView, isChecked) -> loadDebts());

        btnFilterDate.setOnClickListener(v -> showDateRangePicker());
        btnClearFilter.setOnClickListener(v -> {
            filterStartDate = 0;
            filterEndDate = 0;
            btnFilterDate.setText("Filter by Date");
            loadDebts();
        });
    }

    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            filterStartDate = startCal.getTimeInMillis();

            new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                filterEndDate = endCal.getTimeInMillis();

                btnFilterDate.setText(dateFormat.format(filterStartDate) + " - " + dateFormat.format(filterEndDate));
                loadDebts();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadDebts() {
        String searchQuery = etSearch.getText().toString().trim();
        boolean includeCleared = cbShowCleared.isChecked();

        List<Debt> debts = db.searchDebts(searchQuery, filterStartDate, filterEndDate, includeCleared);
        adapter.setDebts(debts);

        tvActiveDebts.setText(String.valueOf(debts.stream().filter(d -> !d.isCleared()).count()));
    }

    private void updateTotals() {
        double totalUSD = db.getTotalActiveDebtUSD();
        double totalZWG = db.getTotalActiveDebtZWG();

        tvTotalDebtUSD.setText(String.format("$%.2f", totalUSD));
        tvTotalDebtZWG.setText(String.format("ZWG %.2f", totalZWG));
    }

    private void clearDebt(Debt debt) {
        new AlertDialog.Builder(this)
                .setTitle("Clear Debt")
                .setMessage("Mark this debt as cleared?\n\n" + debt.getCustomerName() +
                        "\n$" + debt.getAmountUSD() + " / ZWG " + debt.getAmountZWG())
                .setPositiveButton("Clear", (dialog, which) -> {
                    db.clearDebt(debt.getLocalId());
                    Toast.makeText(this, "Debt cleared", Toast.LENGTH_SHORT).show();
                    loadDebts();
                    updateTotals();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {
        private List<Debt> debts = new ArrayList<>();

        void setDebts(List<Debt> debts) {
            this.debts = debts;
            notifyDataSetChanged();
        }

        @Override
        public DebtViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_debt, parent, false);
            return new DebtViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DebtViewHolder holder, int position) {
            Debt debt = debts.get(position);

            holder.tvCustomerName.setText(debt.getCustomerName());
            holder.tvAmountUSD.setText("$" + String.format("%.2f", debt.getAmountUSD()));
            holder.tvAmountZWG.setText("ZWG " + String.format("%.2f", debt.getAmountZWG()));
            holder.tvDate.setText(dateFormat.format(new Date(debt.getCreatedAt())));
            holder.tvType.setText(debt.getType().equals("CHANGE_OWED") ? "Change Owed" : "Credit Used");

            if (debt.getNotes() != null && !debt.getNotes().isEmpty()) {
                holder.tvNotes.setVisibility(View.VISIBLE);
                holder.tvNotes.setText(debt.getNotes());
            } else {
                holder.tvNotes.setVisibility(View.GONE);
            }

            if (debt.isCleared()) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("✓ Cleared " + dateFormat.format(new Date(debt.getClearedAt())));
                holder.btnClear.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.6f);
            } else {
                holder.tvStatus.setVisibility(View.GONE);
                holder.btnClear.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1.0f);
                holder.btnClear.setOnClickListener(v -> clearDebt(debt));
            }
        }

        @Override
        public int getItemCount() {
            return debts.size();
        }

        class DebtViewHolder extends RecyclerView.ViewHolder {
            TextView tvCustomerName, tvAmountUSD, tvAmountZWG, tvDate, tvType, tvNotes, tvStatus;
            Button btnClear;

            DebtViewHolder(View itemView) {
                super(itemView);
                tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
                tvAmountUSD = itemView.findViewById(R.id.tv_amount_usd);
                tvAmountZWG = itemView.findViewById(R.id.tv_amount_zwg);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvType = itemView.findViewById(R.id.tv_type);
                tvNotes = itemView.findViewById(R.id.tv_notes);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnClear = itemView.findViewById(R.id.btn_clear);
            }
        }
    }
}