import sqlite3
import os
from contextlib import contextmanager

DATABASE_PATH = os.path.join(os.path.dirname(__file__), 'shopsync.db')

def get_db():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
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
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
            )
        ''')
        
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
        
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_items_shop ON items(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_sales_shop ON sales(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_debts_shop ON debts(shop_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date)')
