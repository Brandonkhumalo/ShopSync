package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.tishanyq.shopsync.Database.DatabaseHelper;
import com.tishanyq.shopsync.Models.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tishanyq.shopsync.R;

import java.util.List;

public class InventoryActivity extends AppCompatActivity {
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private DatabaseHelper db;
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        db = new DatabaseHelper(this);

        spinnerCategory = findViewById(R.id.spinner_category);
        recyclerView = findViewById(R.id.recycler_view);
        fabAdd = findViewById(R.id.fab_add);

        setupCategorySpinner();
        setupRecyclerView();

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddItemActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void setupCategorySpinner() {
        List<String> categories = db.getAllCategories();
        categories.add(0, "All Categories");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadItems() {
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        List<Item> items;

        if (selectedCategory.equals("All Categories")) {
            items = db.getAllCategories().stream()
                    .flatMap(cat -> db.getItemsByCategory(cat).stream())
                    .collect(java.util.stream.Collectors.toList());
        } else {
            items = db.getItemsByCategory(selectedCategory);
        }

        adapter.setItems(items);
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private List<Item> items;

        void setItems(List<Item> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_inventory, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvCategory.setText(item.getCategory());
            holder.tvPriceUSD.setText("$" + item.getPriceUSD());
            holder.tvPriceZWG.setText("ZWG " + item.getPriceZWG());
            holder.tvQuantity.setText("Qty: " + item.getQuantity());
            holder.tvId.setText(item.getId() != null ? "ID: " + item.getId() : "Pending Sync");
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvPriceUSD, tvPriceZWG, tvQuantity, tvId;

            ItemViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvCategory = itemView.findViewById(R.id.tv_category);
                tvPriceUSD = itemView.findViewById(R.id.tv_price_usd);
                tvPriceZWG = itemView.findViewById(R.id.tv_price_zwg);
                tvQuantity = itemView.findViewById(R.id.tv_quantity);
                tvId = itemView.findViewById(R.id.tv_id);
            }
        }
    }
}