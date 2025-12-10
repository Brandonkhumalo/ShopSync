import sqlite3
import psycopg2
from psycopg2.extras import DictCursor
import os
from dotenv import load_dotenv

load_dotenv()

SQLITE_DB = "shopsync.db"
POSTGRES_URL = os.getenv("DATABASE_URL")

sqlite = sqlite3.connect(SQLITE_DB)
sqlite.row_factory = sqlite3.Row
pg = psycopg2.connect(POSTGRES_URL, cursor_factory=DictCursor)
pgc = pg.cursor()

def migrate_table(table):
    print(f"Migrating: {table} ...")

    rows = sqlite.execute(f"SELECT * FROM {table}").fetchall()
    if not rows:
        print(f"No rows in {table}")
        return

    columns = rows[0].keys()
    col_list = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))

    for row in rows:
        values = [row[c] for c in columns]
        pgc.execute(
            f"INSERT INTO {table} ({col_list}) VALUES ({placeholders}) "
            "ON CONFLICT DO NOTHING",
            values
        )

    pg.commit()
    print(f"Done: {table}")


TABLES = [
    "shops",
    "items",
    "sales",
    "debts",
    "product_keys",
    "shop_devices",
    "sync_logs"
]

for t in TABLES:
    migrate_table(t)

print("Migration completed successfully!")
