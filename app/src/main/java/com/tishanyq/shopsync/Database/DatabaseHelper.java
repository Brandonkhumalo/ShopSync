package com.tishanyq.shopsync.Database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.tishanyq.shopsync.Models.*;
import com.tishanyq.shopsync.Models.Item;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shopsync.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String PREFS_NAME = "ShopSyncPrefs";
    private static final String PREF_APP_ID = "app_id";
    private static final String PREF_SHOP_ID = "shop_id";
    private static final String PREF_DEVICE_SLOT = "device_slot";
    private static final String PREF_EXPIRES_AT = "expires_at";
    private static final String PREF_ACTIVATED_AT = "activated_at";
    private static final String PREF_IS_ACTIVATED = "is_activated";
    
    private Context context;

    // Tables
    private static final String TABLE_SHOP = "shop";
    private static final String TABLE_ITEMS = "items";
    private static final String TABLE_SALES = "sales";
    private static final String TABLE_UNSYNCED = "unsynced_data";
    private static final String TABLE_ANALYTICS = "analytics";
    private static final String TABLE_SYNC_LOG = "sync_log";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SHOP + " (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT, " +
                "owner_name TEXT, " +
                "owner_surname TEXT, " +
                "phone_number TEXT, " +
                "services TEXT, " +
                "address TEXT, " +
                "pin TEXT, " +
                "synced INTEGER DEFAULT 0)");

        // Items table
        db.execSQL("CREATE TABLE " + TABLE_ITEMS + " (" +
                "local_id TEXT PRIMARY KEY, " +
                "id TEXT, " +
                "name TEXT, " +
                "category TEXT, " +
                "price_usd REAL, " +
                "price_zwg REAL, " +
                "quantity INTEGER, " +
                "synced INTEGER DEFAULT 0, " +
                "created_at INTEGER)");

        // Sales table
        db.execSQL("CREATE TABLE " + TABLE_SALES + " (" +
                "local_id TEXT PRIMARY KEY, " +
                "id TEXT, " +
                "item_id TEXT, " +
                "item_name TEXT, " +
                "quantity INTEGER, " +
                "total_usd REAL, " +
                "total_zwg REAL, " +
                "payment_method TEXT, " +
                "debt_used_usd REAL DEFAULT 0, " +
                "debt_used_zwg REAL DEFAULT 0, " +
                "debt_id TEXT, " +
                "sale_date INTEGER, " +
                "synced INTEGER DEFAULT 0)");

        // Debts table
        db.execSQL("CREATE TABLE debts (" +
                "local_id TEXT PRIMARY KEY, " +
                "id TEXT, " +
                "customer_name TEXT, " +
                "amount_usd REAL, " +
                "amount_zwg REAL, " +
                "type TEXT, " +
                "notes TEXT, " +
                "created_at INTEGER, " +
                "cleared INTEGER DEFAULT 0, " +
                "cleared_at INTEGER, " +
                "synced INTEGER DEFAULT 0)");

        // Unsynced data tracking
        db.execSQL("CREATE TABLE " + TABLE_UNSYNCED + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "table_name TEXT, " +
                "record_id TEXT, " +
                "action TEXT, " +
                "timestamp INTEGER)");

        // Analytics cache (monthly data)
        db.execSQL("CREATE TABLE " + TABLE_ANALYTICS + " (" +
                "month TEXT PRIMARY KEY, " +
                "total_sales_usd REAL, " +
                "total_sales_zwg REAL, " +
                "total_transactions INTEGER, " +
                "data TEXT, " +
                "last_updated INTEGER)");

        // Sync log
        db.execSQL("CREATE TABLE " + TABLE_SYNC_LOG + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sync_date INTEGER, " +
                "status TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_SHOP + " ADD COLUMN pin TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SHOP + " ADD COLUMN app_id TEXT");
        }
    }

    public long saveShop(Shop shop) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", shop.getName());
        values.put("owner_name", shop.getOwnerName());
        values.put("owner_surname", shop.getOwnerSurname());
        values.put("phone_number", shop.getPhoneNumber());
        values.put("services", shop.getServices());
        values.put("address", shop.getAddress());
        values.put("pin", shop.getPin());
        values.put("synced", shop.isSynced() ? 1 : 0);

        long result = db.insert(TABLE_SHOP, null, values);
        if (result != -1) {
            addUnsyncedRecord(TABLE_SHOP, shop.getName(), "INSERT");
        }
        return result;
    }

    public Shop getShop() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHOP, null, null, null, null, null, null);
        Shop shop = null;
        if (cursor.moveToFirst()) {
            shop = new Shop();
            shop.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
            shop.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            shop.setOwnerName(cursor.getString(cursor.getColumnIndexOrThrow("owner_name")));
            shop.setOwnerSurname(cursor.getString(cursor.getColumnIndexOrThrow("owner_surname")));
            shop.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow("phone_number")));
            shop.setServices(cursor.getString(cursor.getColumnIndexOrThrow("services")));
            shop.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            shop.setPin(cursor.getString(cursor.getColumnIndexOrThrow("pin")));
            shop.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1);
        }
        cursor.close();
        return shop;
    }

    public boolean verifyPin(String enteredPin) {
        Shop shop = getShop();
        if (shop != null && shop.getPin() != null) {
            return shop.getPin().equals(enteredPin);
        }
        return false;
    }

    public boolean updateShopPin(String pin) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pin", pin);
        int rowsAffected = db.update(TABLE_SHOP, values, null, null);
        if (rowsAffected > 0) {
            addUnsyncedRecord(TABLE_SHOP, "shop", "UPDATE");
        }
        return rowsAffected > 0;
    }

    public void updateShopId(String shopId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", shopId);
        values.put("synced", 1);
        db.update(TABLE_SHOP, values, null, null);
    }

    // Item operations
    public long addItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("local_id", item.getLocalId());
        values.put("name", item.getName());
        values.put("category", item.getCategory());
        values.put("price_usd", item.getPriceUSD());
        values.put("price_zwg", item.getPriceZWG());
        values.put("quantity", item.getQuantity());
        values.put("synced", item.isSynced() ? 1 : 0);
        values.put("created_at", item.getCreatedAt());

        long result = db.insert(TABLE_ITEMS, null, values);
        if (result != -1) {
            addUnsyncedRecord(TABLE_ITEMS, item.getLocalId(), "INSERT");
        }
        return result;
    }

    public List<Item> getItemsByCategory(String category) {
        List<Item> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS, null, "category = ?",
                new String[]{category}, null, null, "name ASC");

        while (cursor.moveToNext()) {
            items.add(cursorToItem(cursor));
        }
        cursor.close();
        return items;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT category FROM " + TABLE_ITEMS + " ORDER BY category", null);

        while (cursor.moveToNext()) {
            categories.add(cursor.getString(0));
        }
        cursor.close();
        return categories;
    }

    public Item getItemByLocalId(String localId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS, null, "local_id = ?",
                new String[]{localId}, null, null, null);
        Item item = null;
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor);
        }
        cursor.close();
        return item;
    }

    public void updateItemQuantity(String localId, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("quantity", newQuantity);
        db.update(TABLE_ITEMS, values, "local_id = ?", new String[]{localId});
        addUnsyncedRecord(TABLE_ITEMS, localId, "UPDATE");
    }

    public void deleteItem(String localId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, "local_id = ?", new String[]{localId});
        addUnsyncedRecord(TABLE_ITEMS, localId, "DELETE");
    }

    // Sale operations
    public long addSale(Sale sale) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("local_id", sale.getLocalId());
        values.put("item_id", sale.getItemId());
        values.put("item_name", sale.getItemName());
        values.put("quantity", sale.getQuantity());
        values.put("total_usd", sale.getTotalUSD());
        values.put("total_zwg", sale.getTotalZWG());
        values.put("payment_method", sale.getPaymentMethod());
        values.put("debt_used_usd", sale.getDebtUsedUSD());
        values.put("debt_used_zwg", sale.getDebtUsedZWG());
        values.put("debt_id", sale.getDebtId());
        values.put("sale_date", sale.getSaleDate());
        values.put("synced", sale.isSynced() ? 1 : 0);

        long result = db.insert(TABLE_SALES, null, values);
        if (result != -1) {
            addUnsyncedRecord(TABLE_SALES, sale.getLocalId(), "INSERT");
        }
        return result;
    }

    public List<Sale> getSalesByDateRange(long startDate, long endDate) {
        List<Sale> sales = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SALES, null,
                "sale_date BETWEEN ? AND ?",
                new String[]{String.valueOf(startDate), String.valueOf(endDate)},
                null, null, "sale_date DESC");

        while (cursor.moveToNext()) {
            sales.add(cursorToSale(cursor));
        }
        cursor.close();
        return sales;
    }

    // Debt operations
    public long addDebt(Debt debt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("local_id", debt.getLocalId());
        values.put("customer_name", debt.getCustomerName());
        values.put("amount_usd", debt.getAmountUSD());
        values.put("amount_zwg", debt.getAmountZWG());
        values.put("type", debt.getType());
        values.put("notes", debt.getNotes());
        values.put("created_at", debt.getCreatedAt());
        values.put("cleared", debt.isCleared() ? 1 : 0);
        values.put("cleared_at", debt.getClearedAt());
        values.put("synced", debt.isSynced() ? 1 : 0);

        long result = db.insert("debts", null, values);
        if (result != -1) {
            addUnsyncedRecord("debts", debt.getLocalId(), "INSERT");
        }
        return result;
    }

    public List<Debt> getActiveDebts() {
        List<Debt> debts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("debts", null, "cleared = 0", null, null, null, "created_at DESC");

        while (cursor.moveToNext()) {
            debts.add(cursorToDebt(cursor));
        }
        cursor.close();
        return debts;
    }

    public List<Debt> searchDebts(String customerName, long startDate, long endDate, boolean includeCleared) {
        List<Debt> debts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        if (!customerName.isEmpty()) {
            selection.append("customer_name LIKE ? ");
            selectionArgs.add("%" + customerName + "%");
        }

        if (startDate > 0 && endDate > 0) {
            if (selection.length() > 0) selection.append("AND ");
            selection.append("created_at BETWEEN ? AND ? ");
            selectionArgs.add(String.valueOf(startDate));
            selectionArgs.add(String.valueOf(endDate));
        }

        if (!includeCleared) {
            if (selection.length() > 0) selection.append("AND ");
            selection.append("cleared = 0");
        }

        Cursor cursor = db.query("debts", null,
                selection.length() > 0 ? selection.toString() : null,
                selectionArgs.size() > 0 ? selectionArgs.toArray(new String[0]) : null,
                null, null, "created_at DESC");

        while (cursor.moveToNext()) {
            debts.add(cursorToDebt(cursor));
        }
        cursor.close();
        return debts;
    }

    public void clearDebt(String localId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("cleared", 1);
        values.put("cleared_at", System.currentTimeMillis());
        db.update("debts", values, "local_id = ?", new String[]{localId});
        addUnsyncedRecord("debts", localId, "UPDATE");
    }

    public Debt getDebtByCustomerName(String customerName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("debts", null,
                "customer_name = ? AND cleared = 0",
                new String[]{customerName},
                null, null, "created_at DESC", "1");

        Debt debt = null;
        if (cursor.moveToFirst()) {
            debt = cursorToDebt(cursor);
        }
        cursor.close();
        return debt;
    }

    public double getTotalActiveDebtUSD() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount_usd) FROM debts WHERE cleared = 0 AND type = 'CHANGE_OWED'", null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTotalActiveDebtZWG() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount_zwg) FROM debts WHERE cleared = 0 AND type = 'CHANGE_OWED'", null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Unsynced data operations
    private void addUnsyncedRecord(String tableName, String recordId, String action) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("table_name", tableName);
        values.put("record_id", recordId);
        values.put("action", action);
        values.put("timestamp", System.currentTimeMillis());
        db.insert(TABLE_UNSYNCED, null, values);
    }

    public List<String[]> getUnsyncedRecords() {
        List<String[]> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_UNSYNCED, null, null, null, null, null, "timestamp ASC");

        while (cursor.moveToNext()) {
            String[] record = new String[3];
            record[0] = cursor.getString(cursor.getColumnIndexOrThrow("table_name"));
            record[1] = cursor.getString(cursor.getColumnIndexOrThrow("record_id"));
            record[2] = cursor.getString(cursor.getColumnIndexOrThrow("action"));
            records.add(record);
        }
        cursor.close();
        return records;
    }

    public void clearUnsyncedRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_UNSYNCED, null, null);
    }

    // Sync log operations
    public void logSync(boolean success) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_date", System.currentTimeMillis());
        values.put("status", success ? "SUCCESS" : "FAILED");
        db.insert(TABLE_SYNC_LOG, null, values);
    }

    public long getLastSyncTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(sync_date) FROM " + TABLE_SYNC_LOG +
                " WHERE status = 'SUCCESS'", null);
        long lastSync = 0;
        if (cursor.moveToFirst()) {
            lastSync = cursor.getLong(0);
        }
        cursor.close();
        return lastSync;
    }

    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, null, null);
        db.delete(TABLE_SALES, null, null);
        db.delete(TABLE_UNSYNCED, null, null);
        db.delete(TABLE_ANALYTICS, null, null);
    }

    // Helper methods
    private Item cursorToItem(Cursor cursor) {
        Item item = new Item();
        item.setLocalId(cursor.getString(cursor.getColumnIndexOrThrow("local_id")));
        item.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        item.setPriceUSD(cursor.getDouble(cursor.getColumnIndexOrThrow("price_usd")));
        item.setPriceZWG(cursor.getDouble(cursor.getColumnIndexOrThrow("price_zwg")));
        item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
        item.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1);
        item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return item;
    }

    private Sale cursorToSale(Cursor cursor) {
        Sale sale = new Sale();
        sale.setLocalId(cursor.getString(cursor.getColumnIndexOrThrow("local_id")));
        sale.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        sale.setItemId(cursor.getString(cursor.getColumnIndexOrThrow("item_id")));
        sale.setItemName(cursor.getString(cursor.getColumnIndexOrThrow("item_name")));
        sale.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
        sale.setTotalUSD(cursor.getDouble(cursor.getColumnIndexOrThrow("total_usd")));
        sale.setTotalZWG(cursor.getDouble(cursor.getColumnIndexOrThrow("total_zwg")));
        sale.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow("payment_method")));
        sale.setDebtUsedUSD(cursor.getDouble(cursor.getColumnIndexOrThrow("debt_used_usd")));
        sale.setDebtUsedZWG(cursor.getDouble(cursor.getColumnIndexOrThrow("debt_used_zwg")));
        sale.setDebtId(cursor.getString(cursor.getColumnIndexOrThrow("debt_id")));
        sale.setSaleDate(cursor.getLong(cursor.getColumnIndexOrThrow("sale_date")));
        sale.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1);
        return sale;
    }

    private Debt cursorToDebt(Cursor cursor) {
        Debt debt = new Debt();
        debt.setLocalId(cursor.getString(cursor.getColumnIndexOrThrow("local_id")));
        debt.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        debt.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow("customer_name")));
        debt.setAmountUSD(cursor.getDouble(cursor.getColumnIndexOrThrow("amount_usd")));
        debt.setAmountZWG(cursor.getDouble(cursor.getColumnIndexOrThrow("amount_zwg")));
        debt.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        debt.setNotes(cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        debt.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        debt.setCleared(cursor.getInt(cursor.getColumnIndexOrThrow("cleared")) == 1);
        debt.setClearedAt(cursor.getLong(cursor.getColumnIndexOrThrow("cleared_at")));
        debt.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1);
        return debt;
    }
    
    public void saveAppAuthorization(String appId, String shopId, int deviceSlot, long activatedAt, long expiresAt) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_APP_ID, appId);
        editor.putString(PREF_SHOP_ID, shopId);
        editor.putInt(PREF_DEVICE_SLOT, deviceSlot);
        editor.putLong(PREF_ACTIVATED_AT, activatedAt);
        editor.putLong(PREF_EXPIRES_AT, expiresAt);
        editor.putBoolean(PREF_IS_ACTIVATED, true);
        editor.apply();
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", shopId);
        values.put("app_id", appId);
        db.update(TABLE_SHOP, values, null, null);
    }
    
    public void savePendingRegistration(String appId, String shopId, int deviceSlot) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_APP_ID, appId);
        editor.putString(PREF_SHOP_ID, shopId);
        editor.putInt(PREF_DEVICE_SLOT, deviceSlot);
        editor.putBoolean(PREF_IS_ACTIVATED, false);
        editor.apply();
    }
    
    public String getAppId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_APP_ID, null);
    }
    
    public String getStoredShopId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_SHOP_ID, null);
    }
    
    public int getDeviceSlot() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_DEVICE_SLOT, 0);
    }
    
    public long getExpiresAt() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_EXPIRES_AT, 0);
    }
    
    public boolean isAppActivated() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_IS_ACTIVATED, false);
    }
    
    public boolean isLicenseExpired() {
        long expiresAt = getExpiresAt();
        if (expiresAt == 0) return true;
        return System.currentTimeMillis() > expiresAt;
    }
    
    public void clearAuthorization() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_EXPIRES_AT);
        editor.remove(PREF_ACTIVATED_AT);
        editor.putBoolean(PREF_IS_ACTIVATED, false);
        editor.apply();
    }
}