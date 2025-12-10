import os
import psycopg2
from psycopg2.extras import DictCursor
from contextlib import contextmanager
from werkzeug.security import generate_password_hash
from dotenv import load_dotenv

load_dotenv()

# Railway PostgreSQL connection
DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=DictCursor)

@contextmanager
def get_db_context():
    conn = get_db()
    cursor = conn.cursor()
    try:
        yield cursor
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        cursor.close()
        conn.close()


# ----------------------------
#   ADMIN TABLE + SEED USER
# ----------------------------

def create_admin_tables(cursor):
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS admin_users (
            id SERIAL PRIMARY KEY,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
            last_login_at BIGINT
        )
    """)


def seed_admin_user(cursor):
    cursor.execute("SELECT id FROM admin_users WHERE email=%s", ('shopsyncadmin@gmail.com',))
    if cursor.fetchone() is None:
        password_hash = generate_password_hash('shopsyncadmin123%')
        cursor.execute("""
            INSERT INTO admin_users (email, password_hash)
            VALUES (%s, %s)
        """, ('shopsyncadmin@gmail.com', password_hash))


# ----------------------------
#   CLEAR ALL DATA
# ----------------------------

def clear_all_data(cursor):
    cursor.execute("DELETE FROM sync_logs")
    cursor.execute("DELETE FROM debts")
    cursor.execute("DELETE FROM sales")
    cursor.execute("DELETE FROM items")
    cursor.execute("DELETE FROM shop_devices")
    cursor.execute("DELETE FROM product_keys")
    cursor.execute("DELETE FROM shops")


# ----------------------------
#   INITIALIZE FULL POSTGRES DB
# ----------------------------

def init_db():
    with get_db_context() as (conn, cursor):

        # SHOPS
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS shops (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                owner_surname TEXT NOT NULL,
                phone_number TEXT NOT NULL,
                services TEXT,
                address TEXT,
                pin TEXT,
                app_id TEXT,
                product_key TEXT,
                activated_at BIGINT,
                expires_at BIGINT,
                subscription_start BIGINT,
                subscription_end BIGINT,
                last_payment_date BIGINT,
                payment_status TEXT DEFAULT 'pending',
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
            )
        """)

        # ITEMS
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS items (
                id TEXT PRIMARY KEY,
                local_id TEXT UNIQUE,
                shop_id TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT,
                price_usd REAL DEFAULT 0,
                price_zwg REAL DEFAULT 0,
                quantity INTEGER DEFAULT 0,
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # SALES
        cursor.execute("""
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
                sale_date BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # DEBTS
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS debts (
                id TEXT PRIMARY KEY,
                local_id TEXT UNIQUE,
                shop_id TEXT NOT NULL,
                customer_name TEXT NOT NULL,
                amount_usd REAL DEFAULT 0,
                amount_zwg REAL DEFAULT 0,
                type TEXT DEFAULT 'CREDIT_USED',
                notes TEXT,
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                cleared INTEGER DEFAULT 0,
                cleared_at BIGINT,
                updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # SYNC LOGS
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS sync_logs (
                id SERIAL PRIMARY KEY,
                shop_id TEXT NOT NULL,
                sync_time BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                success INTEGER DEFAULT 1,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # PRODUCT KEYS
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS product_keys (
                id TEXT PRIMARY KEY,
                product_key TEXT UNIQUE NOT NULL,
                status TEXT DEFAULT 'unused',
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                activated_at BIGINT,
                expires_at BIGINT,
                shop_id TEXT,
                app_id TEXT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # SHOP DEVICES
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS shop_devices (
                id TEXT PRIMARY KEY,
                app_id TEXT UNIQUE NOT NULL,
                shop_id TEXT NOT NULL,
                device_slot INTEGER NOT NULL,
                status TEXT DEFAULT 'pending',
                product_key TEXT,
                registered_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                activated_at BIGINT,
                expires_at BIGINT,
                last_seen BIGINT,
                FOREIGN KEY (shop_id) REFERENCES shops(id)
            )
        """)

        # INDEXES
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_items_shop ON items(shop_id)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_sales_shop ON sales(shop_id)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_debts_shop ON debts(shop_id)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_product_keys_status ON product_keys(status)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_shop_devices_shop ON shop_devices(shop_id)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_shop_devices_app ON shop_devices(app_id)")

        # ADMIN SETUP
        create_admin_tables(cursor)
        seed_admin_user(cursor)
