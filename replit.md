# ShopSync Backend API

## Overview

ShopSync is a Flask-based REST API backend for an Android shop management application. The system handles inventory management, sales tracking, debt management, and multi-device shop synchronization. It uses SQLite as the data store and provides endpoints for shops to manage their products, record transactions, track customer debts, and synchronize data across multiple devices.

**Admin Dashboard**: A React-based admin panel is available at `/admin` for managing shops, product keys, and viewing system statistics.

## Recent Changes

- **2024-12-08**: Added Terms & Conditions screen to Android app (TermsActivity) with $10/month subscription agreement
- **2024-12-08**: Added product key validation on first launch when internet is available
- **2024-12-08**: Added subscription tracking system (subscription_start, subscription_end, payment_status fields)
- **2024-12-08**: Added Subscriptions tab to admin dashboard with payment tracking and "Mark Paid" functionality
- **2024-12-08**: Added React admin dashboard at `/admin` with JWT authentication
- **2024-12-08**: Added database schema migrations for missing columns (app_id, product_key, activated_at, expires_at)
- **2024-12-08**: Created admin account: shopsyncadmin@gmail.com
- **2024-12-08**: Added admin API endpoints for product key management, shops listing, and device monitoring

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Backend Framework
- **Flask (Python 3.11)** - Chosen for its lightweight, flexible nature and ease of rapid development
- Runs on port 5000 by default
- RESTful API design pattern for consistent endpoint structure
- **JWT-based admin authentication** for admin dashboard access
- Device-level validation via app_id/shop_id pairs for mobile app

### Data Storage
- **SQLite3** - Embedded relational database chosen for:
  - Zero-configuration deployment
  - File-based storage (shopsync.db)
  - Built-in support for foreign key constraints
  - Adequate for single-shop or small-scale deployments
- Database context manager pattern (`get_db_context()`) ensures proper connection handling and automatic commit/rollback
- `PRAGMA foreign_keys = ON` enforces referential integrity

### Database Schema Design
Key tables identified:
- **shops** - Shop registration with owner details, contact info, services, and PIN for security
- **items** - Inventory management (structure incomplete in provided files)
- **shop_devices** - Multi-device management tracking app_id, shop_id, device status, and expiration
- Foreign key relationships enforce data integrity between shops and their related entities

### Security & Authorization
- **Device-level authorization** via `require_valid_app` decorator:
  - Validates X-App-Id header or app_id in request body
  - Checks device registration status and expiration against shop_devices table
  - Returns 401 for missing credentials, 403 for unregistered devices
- **Product key system** - Generates activation keys in format XXXX-XXXX-XXXX-XXXX
- **30-day expiration model** for device activations
- PIN-based shop access (stored in shops table)

### API Architecture
RESTful resource-based routing:
- `/api/health` - Health check endpoint
- `/api/shops/*` - Shop management (CRUD operations)
- `/api/shops/<shop_id>/items/*` - Inventory management with category filtering
- `/api/shops/<shop_id>/sales/*` - Sales tracking with date range reporting
- `/api/shops/<shop_id>/debts/*` - Debt management with clear/paid functionality
- `/api/product-keys` - Product key creation and availability
- `/api/shops/<shop_id>/product-keys/activate` - Key activation for devices
- `/api/shops/<shop_id>/devices` - Device registration (max 3 per shop) and listing
- `/api/shops/<shop_id>/devices/<app_id>/status` - Device license status
- `/api/shops/<shop_id>/devices/<app_id>/renew` - License renewal with new key

Design decisions:
- Shop-scoped resources (all items, sales, debts belong to a specific shop)
- Query parameters for filtering (categories, date ranges, cleared debts)
- Dedicated endpoints for complex operations (sales reports, debt clearing)
- Strict 30-day license validity using timedelta (not calendar month)

### Data Identifiers
- **UUID-based IDs** - 12-character hex strings with optional prefixes
- **Timestamp tracking** - Millisecond precision Unix timestamps for created_at/updated_at
- Standardized ID generation via `generate_id()` utility

### JSON Response Format
- `app.config['JSON_SORT_KEYS'] = False` - Preserves insertion order in responses
- `app.url_map.strict_slashes = False` - Flexible URL handling (trailing slash optional)

## External Dependencies

### Python Packages
- **Flask** - Web framework for REST API (only external dependency listed in requirements.txt)

### Mobile Client
- **Android application (Java)** - Consumes the REST API
- Located in `app/` directory alongside backend
- Communicates via HTTP requests with X-App-Id header for device identification

### Database
- **SQLite3** - Bundled with Python, no external server required
- Database file: `shopsync.db` (auto-created on first run via `init_db()`)

### Admin Dashboard (React Frontend)
Located in `frontend/` directory:
- **React + Vite** - Fast build tooling and development experience
- Built to `frontend/dist/` and served by Flask at `/admin`
- Features:
  - Admin login with JWT authentication
  - Overview dashboard with system statistics (including unpaid/paid shops count)
  - Product key management (view all, generate new)
  - Shops listing with device/item/sales counts
  - Device monitoring with status tracking
  - **Subscriptions tab** - Track subscription payments, show start/end dates, mark as paid

### Admin API Endpoints
- `POST /api/admin/login` - Admin authentication, returns JWT token
- `GET /api/admin/stats` - System statistics (shops, devices, product keys, unpaid/paid shops)
- `GET /api/admin/product-keys` - List all product keys with status
- `POST /api/admin/product-keys` - Generate new product key
- `GET /api/admin/shops` - List all shops with counts
- `GET /api/admin/devices` - List all registered devices
- `GET /api/admin/subscriptions` - List all shops with subscription status and dates
- `POST /api/admin/subscriptions/<shop_id>/mark-paid` - Mark subscription as paid (extends 30 days)

### Mobile App Flow
1. **Splash Screen** - Shows app logo for 2 seconds
2. **Terms & Conditions** - On first launch, user must accept $10/month subscription terms
3. **Product Key Validation** - On first internet connection, validates product key with server
4. **Registration/Login** - If no shop exists, goes to registration; otherwise PIN login

### Admin Credentials
- Email: shopsyncadmin@gmail.com
- Password: shopsyncadmin123%

### Potential Future Integrations
The architecture suggests these systems may be integrated later:
- Payment processing for debt clearance
- SMS/notification services for debt reminders (phone numbers stored in shops table)
- Cloud sync services for multi-device data synchronization beyond local database
- Analytics or reporting tools for sales insights