# ShopSync Backend API

## Overview
This is a Flask backend API for the ShopSync Android application. It provides REST API endpoints for syncing shop data including inventory, sales, and customer debts.

## Tech Stack
- **Backend**: Python 3.11, Flask
- **Database**: SQLite3
- **Port**: 5000

## Project Structure
```
backend/
├── app.py          # Main Flask application with all API endpoints
├── database.py     # SQLite database setup and helper functions
└── shopsync.db     # SQLite database file (auto-created)

app/                # Android application (Java)
├── src/main/java/  # Java source files
└── src/main/res/   # Android resources
```

## API Endpoints

### Health Check
- `GET /api/health` - Check API status

### Shops
- `POST /api/shops` - Register a new shop
- `GET /api/shops/<shop_id>` - Get shop details
- `PUT /api/shops/<shop_id>` - Update shop details

### Items (Inventory)
- `GET /api/shops/<shop_id>/items` - List all items (optional: `?category=`)
- `POST /api/shops/<shop_id>/items` - Create a new item
- `PUT /api/shops/<shop_id>/items/<item_id>` - Update an item
- `DELETE /api/shops/<shop_id>/items/<item_id>` - Delete an item
- `GET /api/shops/<shop_id>/categories` - List all categories

### Sales
- `GET /api/shops/<shop_id>/sales` - List sales (optional: `?start_date=&end_date=`)
- `POST /api/shops/<shop_id>/sales` - Record a new sale
- `GET /api/shops/<shop_id>/sales/report` - Get sales report with summary

### Debts
- `GET /api/shops/<shop_id>/debts` - List debts (optional: `?include_cleared=true`)
- `POST /api/shops/<shop_id>/debts` - Create a new debt
- `PUT /api/shops/<shop_id>/debts/<debt_id>` - Update a debt
- `DELETE /api/shops/<shop_id>/debts/<debt_id>` - Delete a debt
- `POST /api/shops/<shop_id>/debts/<debt_id>/clear` - Clear (mark as paid) a debt
- `GET /api/shops/<shop_id>/debts/summary` - Get debts summary

### Sync
- `POST /api/shops/<shop_id>/sync` - Bulk sync data from Android app
- `GET /api/shops/<shop_id>/sync/status` - Get last sync status

## Running the Backend
The backend runs automatically via the configured workflow on port 5000.

### Authentication
- `POST /api/shops/<shop_id>/verify-pin` - Verify shop PIN for login

## Database Schema
- **shops**: Store information (id, name, owner details, address, pin)
- **items**: Inventory items with dual currency pricing (USD/ZWG)
- **sales**: Sales transactions with payment methods
- **debts**: Customer debts tracking
- **sync_logs**: Sync operation history

## Android App Authentication Flow
1. **New Users**: SplashActivity -> RegisterActivity (creates PIN) -> HomeActivity
2. **Existing Users with PIN**: SplashActivity -> PinLoginActivity -> HomeActivity
3. **Existing Users without PIN**: SplashActivity -> SetupPinActivity -> HomeActivity

The PIN is stored locally for offline login and synced to the backend.

## Recent Changes
- Added PIN authentication for login (Dec 2024)
  - PIN field added to Shop model and database
  - PinLoginActivity for existing users
  - SetupPinActivity for users upgrading from old version
  - RegisterActivity updated with PIN creation
  - Backend verify-pin endpoint added
- Initial backend creation (Dec 2024)
