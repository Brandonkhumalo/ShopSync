package com.tishanyq.shopsync.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Debt;
import com.tishanyq.shopsync.Models.Item;
import com.tishanyq.shopsync.Models.Sale;
import com.google.android.material.textfield.TextInputEditText;
import com.tishanyq.shopsync.R;

import java.util.*;

public class SalesActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView recyclerView;
    private TextView tvTotal, tvCart;
    private Button btnCheckout;
    private DatabaseHelper db;
    private SalesAdapter adapter;
    private List<Item> allItems;
    private Map<String, Integer> cart = new HashMap<>();
    private double totalUSD = 0, totalZWG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        db = new DatabaseHelper(this);

        etSearch = findViewById(R.id.et_search);
        recyclerView = findViewById(R.id.recycler_view);
        tvTotal = findViewById(R.id.tv_total);
        tvCart = findViewById(R.id.tv_cart);
        btnCheckout = findViewById(R.id.btn_checkout);

        setupRecyclerView();
        loadAllItems();
        setupSearch();

        btnCheckout.setOnClickListener(v -> showCheckoutOptions());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalesAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadAllItems() {
        allItems = new ArrayList<>();
        List<String> categories = db.getAllCategories();
        for (String category : categories) {
            allItems.addAll(db.getItemsByCategory(category));
        }
        adapter.setItems(allItems);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterItems(String query) {
        List<Item> filtered = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void addToCart(Item item) {
        String itemId = item.getLocalId();
        int currentQty = cart.getOrDefault(itemId, 0);

        if (currentQty < item.getQuantity()) {
            cart.put(itemId, currentQty + 1);
            updateTotal();
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotal() {
        totalUSD = 0;
        totalZWG = 0;
        int itemCount = 0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Item item = db.getItemByLocalId(entry.getKey());
            if (item != null) {
                int qty = entry.getValue();
                totalUSD += item.getPriceUSD() * qty;
                totalZWG += item.getPriceZWG() * qty;
                itemCount += qty;
            }
        }

        tvTotal.setText(String.format("Total: $%.2f / ZWG %.2f", totalUSD, totalZWG));
        tvCart.setText("Cart: " + itemCount + " items");
    }

    private void showCheckoutOptions() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for active debts
        List<Debt> activeDebts = db.getActiveDebts();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Complete Sale");
        builder.setMessage(String.format("Total: $%.2f / ZWG %.2f\n\nChoose payment option:", totalUSD, totalZWG));

        // Option 1: Use customer debt
        if (!activeDebts.isEmpty()) {
            builder.setNeutralButton("Use Customer Debt", (dialog, which) -> showDebtSelection(activeDebts));
        }

        // Option 2: Cash payment
        builder.setPositiveButton("Cash Payment", (dialog, which) -> handleCashPayment());

        // Option 3: Cancel
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showDebtSelection(List<Debt> debts) {
        String[] debtOptions = new String[debts.size()];
        for (int i = 0; i < debts.size(); i++) {
            Debt debt = debts.get(i);
            debtOptions[i] = debt.getCustomerName() + " - $" + debt.getAmountUSD() + " / ZWG " + debt.getAmountZWG();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Customer")
                .setItems(debtOptions, (dialog, which) -> {
                    Debt selectedDebt = debts.get(which);
                    applyDebtToSale(selectedDebt);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyDebtToSale(Debt debt) {
        double debtUsedUSD = Math.min(debt.getAmountUSD(), totalUSD);
        double debtUsedZWG = Math.min(debt.getAmountZWG(), totalZWG);

        double remainingUSD = totalUSD - debtUsedUSD;
        double remainingZWG = totalZWG - debtUsedZWG;

        String message = String.format(
                "Debt Applied:\n$%.2f / ZWG %.2f\n\nRemaining to pay:\n$%.2f / ZWG %.2f",
                debtUsedUSD, debtUsedZWG, remainingUSD, remainingZWG);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Sale with Debt")
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    processSaleWithDebt(debt.getLocalId(), debtUsedUSD, debtUsedZWG);

                    // Clear or update debt
                    if (debtUsedUSD >= debt.getAmountUSD() && debtUsedZWG >= debt.getAmountZWG()) {
                        db.clearDebt(debt.getLocalId());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleCashPayment() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cash_payment, null);
        EditText etPaidUSD = dialogView.findViewById(R.id.et_paid_usd);
        EditText etPaidZWG = dialogView.findViewById(R.id.et_paid_zwg);
        TextView tvChangeUSD = dialogView.findViewById(R.id.tv_change_usd);
        TextView tvChangeZWG = dialogView.findViewById(R.id.tv_change_zwg);

        etPaidUSD.setText(String.valueOf(totalUSD));
        etPaidZWG.setText(String.valueOf(totalZWG));

        TextWatcher changeCalculator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double paidUSD = Double.parseDouble(etPaidUSD.getText().toString());
                    double paidZWG = Double.parseDouble(etPaidZWG.getText().toString());
                    double changeUSD = paidUSD - totalUSD;
                    double changeZWG = paidZWG - totalZWG;
                    tvChangeUSD.setText(String.format("Change: $%.2f", changeUSD));
                    tvChangeZWG.setText(String.format("Change: ZWG %.2f", changeZWG));
                } catch (NumberFormatException e) {
                    tvChangeUSD.setText("Change: $0.00");
                    tvChangeZWG.setText("Change: ZWG 0.00");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPaidUSD.addTextChangedListener(changeCalculator);
        etPaidZWG.addTextChangedListener(changeCalculator);

        new AlertDialog.Builder(this)
                .setTitle("Cash Payment")
                .setView(dialogView)
                .setPositiveButton("Complete", (dialog, which) -> {
                    try {
                        double paidUSD = Double.parseDouble(etPaidUSD.getText().toString());
                        double paidZWG = Double.parseDouble(etPaidZWG.getText().toString());
                        double changeUSD = paidUSD - totalUSD;
                        double changeZWG = paidZWG - totalZWG;

                        if (changeUSD > 0 || changeZWG > 0) {
                            askToSaveAsDebt(changeUSD, changeZWG);
                        } else {
                            processSale();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askToSaveAsDebt(double changeUSD, double changeZWG) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_debt, null);
        EditText etCustomerName = dialogView.findViewById(R.id.et_customer_name);
        TextView tvChangeAmount = dialogView.findViewById(R.id.tv_change_amount);

        tvChangeAmount.setText(String.format("Change: $%.2f / ZWG %.2f", changeUSD, changeZWG));

        new AlertDialog.Builder(this)
                .setTitle("No Change Available")
                .setMessage("Shop doesn't have change. Save as debt?")
                .setView(dialogView)
                .setPositiveButton("Save as Debt", (dialog, which) -> {
                    String customerName = etCustomerName.getText().toString().trim();
                    if (customerName.isEmpty()) {
                        Toast.makeText(this, "Please enter customer name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Debt debt = new Debt(customerName, changeUSD, changeZWG, "CHANGE_OWED",
                            "Change from sale on " + new Date());
                    db.addDebt(debt);

                    processSale();
                    Toast.makeText(this, "Debt saved for " + customerName, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel Sale", null)
                .show();
    }

    private void processSale() {
        processSaleWithDebt(null, 0, 0);
    }

    private void processSaleWithDebt(String debtId, double debtUsedUSD, double debtUsedZWG) {
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String itemId = entry.getKey();
            int qty = entry.getValue();
            Item item = db.getItemByLocalId(itemId);

            if (item != null) {
                Sale sale = new Sale(item.getLocalId(), item.getName(), qty,
                        item.getPriceUSD() * qty, item.getPriceZWG() * qty);
                sale.setDebtUsedUSD(debtUsedUSD);
                sale.setDebtUsedZWG(debtUsedZWG);
                sale.setDebtId(debtId);
                db.addSale(sale);

                // Update inventory
                db.updateItemQuantity(itemId, item.getQuantity() - qty);
            }
        }

        Toast.makeText(this, "Sale completed!", Toast.LENGTH_SHORT).show();
        cart.clear();
        updateTotal();
        loadAllItems();
    }

    private class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SalesViewHolder> {
        private List<Item> items;

        void setItems(List<Item> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public SalesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sales, parent, false);
            return new SalesViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SalesViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvPrice.setText("$" + item.getPriceUSD() + " / ZWG " + item.getPriceZWG());
            holder.tvStock.setText("Stock: " + item.getQuantity());

            holder.btnAdd.setOnClickListener(v -> addToCart(item));
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class SalesViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvStock;
            Button btnAdd;

            SalesViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvStock = itemView.findViewById(R.id.tv_stock);
                btnAdd = itemView.findViewById(R.id.btn_add);
            }
        }
    }
}
