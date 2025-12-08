import sqlite3
import os
from contextlib import contextmanager
from werkzeug.security import generate_password_hash

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'shopsync.db')

def get_db():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    return conn

@contextmanager
def get_db_context():
    conn = get_db()
    try:
        yield conn
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()

def apply_schema_migrations(cursor):
    """Apply schema migrations to ensure all columns exist"""
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS schema_migrations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            migration_name TEXT UNIQUE NOT NULL,
            applied_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
        )
    ''')
    
    cursor.execute("SELECT migration_name FROM schema_migrations")
    applied = {row[0] for row in cursor.fetchall()}
    
    if 'add_app_id_to_shops' not in applied:
        cursor.execute("PRAGMA table_info(shops)")
        columns = [row[1] for row in cursor.fetchall()]
        if 'app_id' not in columns:
            cursor.execute("ALTER TABLE shops ADD COLUMN app_id TEXT")
        cursor.execute("INSERT INTO schema_migrations (migration_name) VALUES (?)", ('add_app_id_to_shops',))
    
    if 'add_product_key_to_shops' not in applied:
        cursor.execute("PRAGMA table_info(shops)")
        columns = [row[1] for row in cursor.fetchall()]
        if 'product_key' not in columns:
            cursor.execute("ALTER TABLE shops ADD COLUMN product_key TEXT")
        cursor.execute("INSERT INTO schema_migrations (migration_name) VALUES (?)", ('add_product_key_to_shops',))
    
    if 'add_activated_at_to_shops' not in applied:
        cursor.execute("PRAGMA table_info(shops)")
        columns = [row[1] for row in cursor.fetchall()]
        if 'activated_at' not in columns:
            cursor.execute("ALTER TABLE shops ADD COLUMN activated_at INTEGER")
        if 'expires_at' not in columns:
            cursor.execute("ALTER TABLE shops ADD COLUMN expires_at INTEGER")
        cursor.execute("INSERT INTO schema_migrations (migration_name) VALUES (?)", ('add_activated_at_to_shops',))

def create_admin_tables(cursor):
    """Create admin-related tables"""
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS admin_users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
            last_login_at INTEGER
        )
    ''')

def seed_admin_user(cursor):
    """Create default admin user if not exists"""
    cursor.execute("SELECT id FROM admin_users WHERE email = ?", ('shopsyncadmin@gmail.com',))
    if not cursor.fetchone():
        password_hash = generate_password_hash('shopsyncadmin123%')
        cursor.execute('''
            INSERT INTO admin_users (email, password_hash)
            VALUES (?, ?)
        ''', ('shopsyncadmin@gmail.com', password_hash))

def clear_all_data(cursor):
    """Clear all shops, product keys, and related data"""
    cursor.execute("DELETE FROM sync_logs")
    cursor.execute("DELETE FROM debts")
    cursor.execute("DELETE FROM sales")
    cursor.execute("DELETE FROM items")
    cursor.execute("DELETE FROM shop_devices")
    cursor.execute("DELETE FROM product_keys")
    cursor.execute("DELETE FROM shops")

def init_db():
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS shops (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                owner_surname TEXT NOT NULL,
                phone_number TEXT NOT NULL,
                services TEXT,
                address TEXT,
                pin TEXT,
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
            )
        ''')
        
        cursor.execute("PRAGMA table_info(shops)")
        columns = [row[1] for row in cursor.fetchall()]
        if 'pin' not in columns:
            cursor.execute("ALTER TABLE shops ADD COLUMN pin TEXT")
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS items (
                id TEXT PRIMARY KEY,
                local_id TEXT UNIQUE,
                shop_id TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT,
                price_usd REAL DEFAULT 0,
                price_zwg REAL DEFAULT 0,
                quantity INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS sales (
                id TEXT PRIMARY KEY,
                local_id TEXT UNIQUE,
                shop_id TEXT NOT NULL,
                item_id TEXT,
                item_name TEXT,
                quantity INTEGER DEFAULT 0,
                total_usd REAL DEFAULT 0,
                total_zwg REAL DEFAULT 0,
                payment_method TEXT DEFAULT 'CASH',
                debt_used_usd REAL DEFAULT 0,
                debt_used_zwg REAL DEFAULT 0,
                debt_id TEXT,
                sale_date INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS debts (
                id TEXT PRIMARY KEY,
                local_id TEXT UNIQUE,
                shop_id TEXT NOT NULL,
                customer_name TEXT NOT NULL,
                amount_usd REAL DEFAULT 0,
                amount_zwg REAL DEFAULT 0,
                type TEXT DEFAULT 'CREDIT_USED',
                notes TEXT,
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                cleared INTEGER DEFAULT 0,
                cleared_at INTEGER,
                updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS sync_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                shop_id TEXT NOT NULL,
                sync_time INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                success INTEGER DEFAULT 1,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS product_keys (
                id TEXT PRIMARY KEY,
                product_key TEXT UNIQUE NOT NULL,
                status TEXT DEFAULT 'unused',
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                activated_at INTEGER,
                expires_at INTEGER,
                shop_id TEXT,
                app_id TEXT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS shop_devices (
                id TEXT PRIMARY KEY,
                app_id TEXT UNIQUE NOT NULL,
                shop_id TEXT NOT NULL,
                device_slot INTEGER NOT NULL,
                status TEXT DEFAULT 'pending',
                product_key TEXT,
                registered_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                activated_at INTEGER,
                expires_at INTEGER,
                last_seen INTEGER,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        ''')
        
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_items_shop ON items(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_sales_shop ON sales(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_debts_shop ON debts(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_product_keys_status ON product_keys(status)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_shop_devices_shop ON shop_devices(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_shop_devices_app ON shop_devices(app_id)')
        
        apply_schema_migrations(cursor)
        create_admin_tables(cursor)
        seed_admin_user(cursor)
