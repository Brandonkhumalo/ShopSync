import os
import uuid
import time
import secrets
import string
import jwt
from datetime import datetime, timedelta
from functools import wraps
from flask import Flask, request, jsonify, send_from_directory
from werkzeug.security import check_password_hash
from database import init_db, get_db_context
from flask_cors import CORS

SECRET_KEY = os.environ.get('SESSION_SECRET')
if not SECRET_KEY:
    import secrets as sec_module
    SECRET_KEY = sec_module.token_hex(32)

app = Flask(__name__)
CORS(app)

app.config['JSON_SORT_KEYS'] = False
app.url_map.strict_slashes = False

@app.after_request
def add_cors_headers(response):
    origin = request.headers.get('Origin')
    response.headers['Access-Control-Allow-Origin'] = origin or '*'
    response.headers['Access-Control-Allow-Credentials'] = 'true'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type,Authorization'
    response.headers['Access-Control-Allow-Methods'] = 'GET,POST,PUT,DELETE,OPTIONS'
    return response

def generate_id(prefix=''):
    return f"{prefix}{uuid.uuid4().hex[:12]}"

def get_timestamp():
    return int(time.time() * 1000)

def generate_product_key():
    chars = string.ascii_uppercase + string.digits
    parts = [''.join(secrets.choice(chars) for _ in range(4)) for _ in range(4)]
    return '-'.join(parts)

def calculate_expiry_date(activated_at_ms):
    activated_date = datetime.fromtimestamp(activated_at_ms / 1000)
    expiry_date = activated_date + timedelta(days=30)
    return int(expiry_date.timestamp() * 1000)

def require_valid_app(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        app_id = request.headers.get('X-App-Id') or request.json.get('app_id') if request.json else None
        shop_id = kwargs.get('shop_id')
        
        if not app_id:
            return jsonify({'error': 'App ID is required'}), 401
        
        with get_db_context() as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT status, expires_at FROM shop_devices 
                WHERE app_id = %s AND shop_id = %s
            ''', (app_id, shop_id))
            device = cursor.fetchone()
            
            if not device:
                return jsonify({'error': 'Device not registered'}), 403
            
            if device['status'] != 'active':
                return jsonify({'error': 'Device not activated', 'status': device['status']}), 403
            
            current_time = get_timestamp()
            if device['expires_at'] and device['expires_at'] < current_time:
                return jsonify({'error': 'License expired', 'expired': True}), 403
            
            cursor.execute('''
                UPDATE shop_devices SET last_seen = %s WHERE app_id = %s
            ''', (current_time, app_id))
        
        return f(*args, **kwargs)
    return decorated_function

@app.before_request
def before_request():
    init_db()

@app.route('/', methods=['GET'])
def api_index():
    endpoints = {
        'message': 'ShopSync API - Available Endpoints',
        'whatsapp_support': '+263788539918',
        'endpoints': {
            'Health': {
                'GET /api/health': 'Check API health status'
            },
            'Shops': {
                'POST /api/shops': 'Register a new shop',
                'GET /api/shops/<shop_id>': 'Get shop details',
                'PUT /api/shops/<shop_id>': 'Update shop details',
                'POST /api/shops/<shop_id>/verify-pin': 'Verify shop PIN'
            },
            'Items': {
                'GET /api/shops/<shop_id>/items': 'Get all items (optional ?category=)',
                'POST /api/shops/<shop_id>/items': 'Create a new item',
                'PUT /api/shops/<shop_id>/items/<item_id>': 'Update an item',
                'DELETE /api/shops/<shop_id>/items/<item_id>': 'Delete an item',
                'GET /api/shops/<shop_id>/categories': 'Get all categories'
            },
            'Sales': {
                'GET /api/shops/<shop_id>/sales': 'Get sales (optional ?start_date=&end_date=)',
                'POST /api/shops/<shop_id>/sales': 'Record a sale',
                'GET /api/shops/<shop_id>/sales/report': 'Get sales report'
            },
            'Debts': {
                'GET /api/shops/<shop_id>/debts': 'Get debts (optional ?include_cleared=true)',
                'POST /api/shops/<shop_id>/debts': 'Create a debt',
                'PUT /api/shops/<shop_id>/debts/<debt_id>': 'Update a debt',
                'POST /api/shops/<shop_id>/debts/<debt_id>/clear': 'Clear a debt',
                'DELETE /api/shops/<shop_id>/debts/<debt_id>': 'Delete a debt',
                'GET /api/shops/<shop_id>/debts/summary': 'Get debts summary'
            },
            'Sync': {
                'POST /api/shops/<shop_id>/sync': 'Sync all data',
                'GET /api/shops/<shop_id>/sync/status': 'Get sync status'
            },
            'Product Keys': {
                'POST /api/product-keys': 'Generate a new product key',
                'GET /api/product-keys/available': 'Get available product keys',
                'POST /api/shops/<shop_id>/product-keys/activate': 'Activate a product key'
            },
            'Devices': {
                'GET /api/shops/<shop_id>/devices': 'Get all registered devices (max 3)',
                'POST /api/shops/<shop_id>/devices': 'Register a new device',
                'GET /api/shops/<shop_id>/devices/<app_id>/status': 'Get device license status',
                'POST /api/shops/<shop_id>/devices/<app_id>/renew': 'Renew device license',
                'GET /api/shops/<shop_id>/devices/<app_id>/license-info': 'Get license info with product key'
            }
        }
    }
    return jsonify(endpoints)

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'ok', 'message': 'ShopSync API is running'})

@app.route('/api/shops', methods=['POST'])
def register_shop():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    required = ['name', 'owner_name', 'owner_surname', 'phone_number']
    for field in required:
        if not data.get(field):
            return jsonify({'error': f'{field} is required'}), 400
    
    shop_id = generate_id('SHOP_')
    app_id = generate_id('APP_')
    device_id = generate_id('DEV_')
    current_time = get_timestamp()
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO shops (id, name, owner_name, owner_surname, phone_number, services, address, pin)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        ''', (
            shop_id,
            data.get('name'),
            data.get('owner_name'),
            data.get('owner_surname'),
            data.get('phone_number'),
            data.get('services', ''),
            data.get('address', ''),
            data.get('pin')
        ))
        
        cursor.execute('''
            INSERT INTO shop_devices (id, app_id, shop_id, device_slot, status, registered_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        ''', (device_id, app_id, shop_id, 1, 'pending', current_time))
    
    return jsonify({
        'id': shop_id,
        'shop_id': shop_id,
        'app_id': app_id,
        'device_slot': 1,
        'message': 'Shop registered successfully. Please activate with a product key.'
    }), 201

@app.route('/api/shops/<shop_id>/verify-pin', methods=['POST'])
def verify_pin(shop_id):
    data = request.get_json()
    if not data or not data.get('pin'):
        return jsonify({'error': 'PIN is required'}), 400
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT pin FROM shops WHERE id = %s', (shop_id,))
        shop = cursor.fetchone()
        
        if not shop:
            return jsonify({'error': 'Shop not found'}), 404
        
        if shop['pin'] == data.get('pin'):
            return jsonify({'valid': True, 'message': 'PIN verified successfully'})
        else:
            return jsonify({'valid': False, 'message': 'Invalid PIN'}), 401

@app.route('/api/shops/<shop_id>', methods=['GET'])
def get_shop(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM shops WHERE id = %s', (shop_id,))
        shop = cursor.fetchone()
        
        if not shop:
            return jsonify({'error': 'Shop not found'}), 404
        
        return jsonify(dict(shop))

@app.route('/api/shops/<shop_id>', methods=['PUT'])
def update_shop(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            UPDATE shops SET
                name = COALESCE(%s, name),
                owner_name = COALESCE(%s, owner_name),
                owner_surname = COALESCE(%s, owner_surname),
                phone_number = COALESCE(%s, phone_number),
                services = COALESCE(%s, services),
                address = COALESCE(%s, address),
                updated_at = %s
            WHERE id = %s
        ''', (
            data.get('name'),
            data.get('owner_name'),
            data.get('owner_surname'),
            data.get('phone_number'),
            data.get('services'),
            data.get('address'),
            get_timestamp(),
            shop_id
        ))
    
    return jsonify({'message': 'Shop updated successfully'})

@app.route('/api/shops/<shop_id>/items', methods=['GET'])
def get_items(shop_id):
    category = request.args.get('category')
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        if category:
            cursor.execute('SELECT * FROM items WHERE shop_id = %s AND category = %s', (shop_id, category))
        else:
            cursor.execute('SELECT * FROM items WHERE shop_id = %s', (shop_id,))
        
        items = [dict(row) for row in cursor.fetchall()]
        return jsonify(items)

@app.route('/api/shops/<shop_id>/items', methods=['POST'])
def create_item(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    if not data.get('name'):
        return jsonify({'error': 'Item name is required'}), 400
    
    item_id = generate_id('ITEM_')
    local_id = data.get('local_id', item_id)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO items (id, local_id, shop_id, name, category, price_usd, price_zwg, quantity, created_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''', (
            item_id,
            local_id,
            shop_id,
            data.get('name'),
            data.get('category', 'General'),
            data.get('price_usd', 0),
            data.get('price_zwg', 0),
            data.get('quantity', 0),
            data.get('created_at', get_timestamp())
        ))
    
    return jsonify({'id': item_id, 'local_id': local_id, 'message': 'Item created successfully'}), 201

@app.route('/api/shops/<shop_id>/items/<item_id>', methods=['PUT'])
def update_item(shop_id, item_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM items WHERE (id = %s OR local_id = %s) AND shop_id = %s', (item_id, item_id, shop_id))
        if not cursor.fetchone():
            return jsonify({'error': 'Item not found'}), 404
        
        cursor.execute('''
            UPDATE items SET
                name = COALESCE(%s, name),
                category = COALESCE(%s, category),
                price_usd = COALESCE(%s, price_usd),
                price_zwg = COALESCE(%s, price_zwg),
                quantity = COALESCE(%s, quantity),
                updated_at = %s
            WHERE (id = %s OR local_id = %s) AND shop_id = %s
        ''', (
            data.get('name'),
            data.get('category'),
            data.get('price_usd'),
            data.get('price_zwg'),
            data.get('quantity'),
            get_timestamp(),
            item_id,
            item_id,
            shop_id
        ))
    
    return jsonify({'message': 'Item updated successfully'})

@app.route('/api/shops/<shop_id>/items/<item_id>', methods=['DELETE'])
def delete_item(shop_id, item_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('DELETE FROM items WHERE (id = %s OR local_id = %s) AND shop_id = %s', (item_id, item_id, shop_id))
        if cursor.rowcount == 0:
            return jsonify({'error': 'Item not found'}), 404
    
    return jsonify({'message': 'Item deleted successfully'})

@app.route('/api/shops/<shop_id>/categories', methods=['GET'])
def get_categories(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT DISTINCT category FROM items WHERE shop_id = %s AND category IS NOT NULL', (shop_id,))
        categories = [row['category'] for row in cursor.fetchall()]
        return jsonify(categories)

@app.route('/api/shops/<shop_id>/sales', methods=['GET'])
def get_sales(shop_id):
    start_date = request.args.get('start_date', type=int)
    end_date = request.args.get('end_date', type=int)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        query = 'SELECT * FROM sales WHERE shop_id = %s'
        params = [shop_id]
        
        if start_date:
            query += ' AND sale_date >= %s'
            params.append(start_date)
        if end_date:
            query += ' AND sale_date <= %s'
            params.append(end_date)
        
        query += ' ORDER BY sale_date DESC'
        cursor.execute(query, params)
        
        sales = [dict(row) for row in cursor.fetchall()]
        return jsonify(sales)

@app.route('/api/shops/<shop_id>/sales', methods=['POST'])
def create_sale(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    sale_id = generate_id('SALE_')
    local_id = data.get('local_id', sale_id)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO sales (id, local_id, shop_id, item_id, item_name, quantity, total_usd, total_zwg,
                             payment_method, debt_used_usd, debt_used_zwg, debt_id, sale_date)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''', (
            sale_id,
            local_id,
            shop_id,
            data.get('item_id'),
            data.get('item_name'),
            data.get('quantity', 0),
            data.get('total_usd', 0),
            data.get('total_zwg', 0),
            data.get('payment_method', 'CASH'),
            data.get('debt_used_usd', 0),
            data.get('debt_used_zwg', 0),
            data.get('debt_id'),
            data.get('sale_date', get_timestamp())
        ))
    
    return jsonify({'id': sale_id, 'local_id': local_id, 'message': 'Sale recorded successfully'}), 201

@app.route('/api/shops/<shop_id>/sales/report', methods=['GET'])
def get_sales_report(shop_id):
    start_date = request.args.get('start_date', type=int)
    end_date = request.args.get('end_date', type=int)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        query = '''
            SELECT 
                COUNT(*) as total_transactions,
                COALESCE(SUM(total_usd), 0) as total_usd,
                COALESCE(SUM(total_zwg), 0) as total_zwg,
                COALESCE(SUM(quantity), 0) as total_items_sold
            FROM sales WHERE shop_id = %s
        '''
        params = [shop_id]
        
        if start_date:
            query += ' AND sale_date >= %s'
            params.append(start_date)
        if end_date:
            query += ' AND sale_date <= %s'
            params.append(end_date)
        
        cursor.execute(query, params)
        summary = dict(cursor.fetchone())
        
        top_items_query = '''
            SELECT item_name, SUM(quantity) as total_qty, SUM(total_usd) as revenue_usd
            FROM sales WHERE shop_id = %s
        '''
        top_params = [shop_id]
        
        if start_date:
            top_items_query += ' AND sale_date >= %s'
            top_params.append(start_date)
        if end_date:
            top_items_query += ' AND sale_date <= %s'
            top_params.append(end_date)
        
        top_items_query += ' GROUP BY item_name ORDER BY total_qty DESC LIMIT 10'
        cursor.execute(top_items_query, top_params)
        top_items = [dict(row) for row in cursor.fetchall()]
        
        return jsonify({
            'summary': summary,
            'top_items': top_items
        })

@app.route('/api/shops/<shop_id>/debts', methods=['GET'])
def get_debts(shop_id):
    include_cleared = request.args.get('include_cleared', 'false').lower() == 'true'
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        if include_cleared:
            cursor.execute('SELECT * FROM debts WHERE shop_id = %s ORDER BY created_at DESC', (shop_id,))
        else:
            cursor.execute('SELECT * FROM debts WHERE shop_id = %s AND cleared = 0 ORDER BY created_at DESC', (shop_id,))
        
        debts = [dict(row) for row in cursor.fetchall()]
        return jsonify(debts)

@app.route('/api/shops/<shop_id>/debts', methods=['POST'])
def create_debt(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    if not data.get('customer_name'):
        return jsonify({'error': 'Customer name is required'}), 400
    
    debt_id = generate_id('DEBT_')
    local_id = data.get('local_id', debt_id)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO debts (id, local_id, shop_id, customer_name, amount_usd, amount_zwg, type, notes, created_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''', (
            debt_id,
            local_id,
            shop_id,
            data.get('customer_name'),
            data.get('amount_usd', 0),
            data.get('amount_zwg', 0),
            data.get('type', 'CREDIT_USED'),
            data.get('notes', ''),
            data.get('created_at', get_timestamp())
        ))
    
    return jsonify({'id': debt_id, 'local_id': local_id, 'message': 'Debt created successfully'}), 201

@app.route('/api/shops/<shop_id>/debts/<debt_id>', methods=['PUT'])
def update_debt(shop_id, debt_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id FROM debts WHERE (id = %s OR local_id = %s) AND shop_id = %s', (debt_id, debt_id, shop_id))
        if not cursor.fetchone():
            return jsonify({'error': 'Debt not found'}), 404
        
        cursor.execute('''
            UPDATE debts SET
                customer_name = COALESCE(%s, customer_name),
                amount_usd = COALESCE(%s, amount_usd),
                amount_zwg = COALESCE(%s, amount_zwg),
                type = COALESCE(%s, type),
                notes = COALESCE(%s, notes),
                updated_at = %s
            WHERE (id = %s OR local_id = %s) AND shop_id = %s
        ''', (
            data.get('customer_name'),
            data.get('amount_usd'),
            data.get('amount_zwg'),
            data.get('type'),
            data.get('notes'),
            get_timestamp(),
            debt_id,
            debt_id,
            shop_id
        ))
    
    return jsonify({'message': 'Debt updated successfully'})

@app.route('/api/shops/<shop_id>/debts/<debt_id>/clear', methods=['POST'])
def clear_debt(shop_id, debt_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            UPDATE debts SET cleared = 1, cleared_at = %s, updated_at = %s
            WHERE (id = %s OR local_id = %s) AND shop_id = %s
        ''', (get_timestamp(), get_timestamp(), debt_id, debt_id, shop_id))
        
        if cursor.rowcount == 0:
            return jsonify({'error': 'Debt not found'}), 404
    
    return jsonify({'message': 'Debt cleared successfully'})

@app.route('/api/shops/<shop_id>/debts/<debt_id>', methods=['DELETE'])
def delete_debt(shop_id, debt_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('DELETE FROM debts WHERE (id = %s OR local_id = %s) AND shop_id = %s', (debt_id, debt_id, shop_id))
        if cursor.rowcount == 0:
            return jsonify({'error': 'Debt not found'}), 404
    
    return jsonify({'message': 'Debt deleted successfully'})

@app.route('/api/shops/<shop_id>/debts/summary', methods=['GET'])
def get_debts_summary(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT 
                COUNT(*) as total_debts,
                SUM(CASE WHEN cleared = 0 THEN 1 ELSE 0 END) as active_debts,
                COALESCE(SUM(CASE WHEN cleared = 0 THEN amount_usd ELSE 0 END), 0) as total_usd,
                COALESCE(SUM(CASE WHEN cleared = 0 THEN amount_zwg ELSE 0 END), 0) as total_zwg
            FROM debts WHERE shop_id = %s
        ''', (shop_id,))
        summary = dict(cursor.fetchone())
        return jsonify(summary)

@app.route('/api/shops/<shop_id>/sync', methods=['POST'])
def sync_data(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    results = {
        'items': {'created': 0, 'updated': 0},
        'sales': {'created': 0},
        'debts': {'created': 0, 'updated': 0}
    }
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        for item in data.get('items', []):
            local_id = item.get('local_id')
            cursor.execute('SELECT id FROM items WHERE local_id = %s AND shop_id = %s', (local_id, shop_id))
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute('''
                    UPDATE items SET name = %s, category = %s, price_usd = %s, price_zwg = %s, quantity = %s, updated_at = %s
                    WHERE local_id = %s AND shop_id = %s
                ''', (item.get('name'), item.get('category'), item.get('price_usd', 0), 
                      item.get('price_zwg', 0), item.get('quantity', 0), get_timestamp(), local_id, shop_id))
                results['items']['updated'] += 1
            else:
                item_id = generate_id('ITEM_')
                cursor.execute('''
                    INSERT INTO items (id, local_id, shop_id, name, category, price_usd, price_zwg, quantity, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ''', (item_id, local_id, shop_id, item.get('name'), item.get('category'),
                      item.get('price_usd', 0), item.get('price_zwg', 0), item.get('quantity', 0),
                      item.get('created_at', get_timestamp())))
                results['items']['created'] += 1
        
        for sale in data.get('sales', []):
            local_id = sale.get('local_id')
            cursor.execute('SELECT id FROM sales WHERE local_id = %s AND shop_id = %s', (local_id, shop_id))
            if not cursor.fetchone():
                sale_id = generate_id('SALE_')
                cursor.execute('''
                    INSERT INTO sales (id, local_id, shop_id, item_id, item_name, quantity, total_usd, total_zwg,
                                      payment_method, debt_used_usd, debt_used_zwg, debt_id, sale_date)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ''', (sale_id, local_id, shop_id, sale.get('item_id'), sale.get('item_name'),
                      sale.get('quantity', 0), sale.get('total_usd', 0), sale.get('total_zwg', 0),
                      sale.get('payment_method', 'CASH'), sale.get('debt_used_usd', 0),
                      sale.get('debt_used_zwg', 0), sale.get('debt_id'), sale.get('sale_date', get_timestamp())))
                results['sales']['created'] += 1
        
        for debt in data.get('debts', []):
            local_id = debt.get('local_id')
            cursor.execute('SELECT id FROM debts WHERE local_id = %s AND shop_id = %s', (local_id, shop_id))
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute('''
                    UPDATE debts SET customer_name = %s, amount_usd = %s, amount_zwg = %s, type = %s, 
                                    notes = %s, cleared = %s, cleared_at = %s, updated_at = %s
                    WHERE local_id = %s AND shop_id = %s
                ''', (debt.get('customer_name'), debt.get('amount_usd', 0), debt.get('amount_zwg', 0),
                      debt.get('type', 'CREDIT_USED'), debt.get('notes', ''), 
                      1 if debt.get('cleared') else 0, debt.get('cleared_at'), get_timestamp(), local_id, shop_id))
                results['debts']['updated'] += 1
            else:
                debt_id = generate_id('DEBT_')
                cursor.execute('''
                    INSERT INTO debts (id, local_id, shop_id, customer_name, amount_usd, amount_zwg, type, notes, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ''', (debt_id, local_id, shop_id, debt.get('customer_name'), debt.get('amount_usd', 0),
                      debt.get('amount_zwg', 0), debt.get('type', 'CREDIT_USED'), debt.get('notes', ''),
                      debt.get('created_at', get_timestamp())))
                results['debts']['created'] += 1
        
        cursor.execute('INSERT INTO sync_logs (shop_id, success) VALUES (%s, 1)', (shop_id,))
    
    return jsonify({
        'message': 'Sync completed successfully',
        'results': results,
        'sync_time': get_timestamp()
    })

@app.route('/api/shops/<shop_id>/sync/status', methods=['GET'])
def get_sync_status(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT sync_time, success FROM sync_logs 
            WHERE shop_id = %s ORDER BY sync_time DESC LIMIT 1
        ''', (shop_id,))
        last_sync = cursor.fetchone()
        
        if last_sync:
            return jsonify({
                'last_sync': last_sync['sync_time'],
                'success': bool(last_sync['success'])
            })
        else:
            return jsonify({'last_sync': None, 'success': None})

@app.route('/api/product-keys', methods=['POST'])
def create_product_key():
    key_id = generate_id('KEY_')
    product_key = generate_product_key()
    current_time = get_timestamp()
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO product_keys (id, product_key, status, created_at)
            VALUES (%s, %s, %s, %s)
        ''', (key_id, product_key, 'unused', current_time))
    
    return jsonify({
        'id': key_id,
        'product_key': product_key,
        'status': 'unused',
        'created_at': current_time,
        'message': 'Product key created successfully'
    }), 201

@app.route('/api/product-keys/available', methods=['GET'])
def get_available_product_keys():
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, product_key, created_at FROM product_keys 
            WHERE status = 'unused' ORDER BY created_at DESC
        ''')
        keys = [dict(row) for row in cursor.fetchall()]
        return jsonify({
            'count': len(keys),
            'keys': keys
        })

@app.route('/api/shops/<shop_id>/product-keys/activate', methods=['POST'])
def activate_product_key(shop_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    product_key = data.get('product_key')
    app_id = data.get('app_id')
    
    if not product_key:
        return jsonify({'error': 'Product key is required'}), 400
    if not app_id:
        return jsonify({'error': 'App ID is required'}), 400
    
    current_time = get_timestamp()
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            SELECT id, app_id, device_slot, status, expires_at FROM shop_devices 
            WHERE app_id = %s AND shop_id = %s
        ''', (app_id, shop_id))
        device = cursor.fetchone()
        
        if not device:
            return jsonify({'error': 'Device not registered for this shop'}), 404
        
        cursor.execute('''
            SELECT id, status FROM product_keys WHERE product_key = %s
        ''', (product_key,))
        key_record = cursor.fetchone()
        
        if not key_record:
            return jsonify({'error': 'Invalid product key'}), 400
        
        if key_record['status'] == 'used':
            return jsonify({'error': 'Product key has already been used'}), 400
        
        expires_at = calculate_expiry_date(current_time)
        
        cursor.execute('''
            UPDATE product_keys SET 
                status = 'used', 
                activated_at = %s, 
                expires_at = %s, 
                shop_id = %s, 
                app_id = %s
            WHERE id = %s
        ''', (current_time, expires_at, shop_id, app_id, key_record['id']))
        
        cursor.execute('''
            UPDATE shop_devices SET 
                status = 'active', 
                product_key = %s, 
                activated_at = %s, 
                expires_at = %s,
                last_seen = %s
            WHERE app_id = %s AND shop_id = %s
        ''', (product_key, current_time, expires_at, current_time, app_id, shop_id))
    
    return jsonify({
        'message': 'Product key activated successfully',
        'app_id': app_id,
        'shop_id': shop_id,
        'device_slot': device['device_slot'],
        'activated_at': current_time,
        'expires_at': expires_at,
        'status': 'active'
    })

@app.route('/api/shops/<shop_id>/devices', methods=['GET'])
def get_shop_devices(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, app_id, device_slot, status, registered_at, activated_at, expires_at, last_seen 
            FROM shop_devices WHERE shop_id = %s ORDER BY device_slot
        ''', (shop_id,))
        devices = [dict(row) for row in cursor.fetchall()]
        
        current_time = get_timestamp()
        for device in devices:
            if device['expires_at'] and device['expires_at'] < current_time:
                device['expired'] = True
            else:
                device['expired'] = False
        
        return jsonify({
            'count': len(devices),
            'max_devices': 3,
            'devices': devices
        })

@app.route('/api/shops/<shop_id>/devices', methods=['POST'])
def register_new_device(shop_id):
    requesting_app_id = request.headers.get('X-App-Id')
    if request.json:
        requesting_app_id = requesting_app_id or request.json.get('requesting_app_id')
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        if requesting_app_id:
            cursor.execute('''
                SELECT status, expires_at FROM shop_devices 
                WHERE app_id = %s AND shop_id = %s AND status = 'active'
            ''', (requesting_app_id, shop_id))
            auth_device = cursor.fetchone()
            
            if not auth_device:
                return jsonify({'error': 'Authentication required. Please use an active device to register new devices.'}), 403
            
            current_time = get_timestamp()
            if auth_device['expires_at'] and auth_device['expires_at'] < current_time:
                return jsonify({'error': 'Your license has expired. Please renew before registering new devices.'}), 403
        else:
            cursor.execute('''
                SELECT COUNT(*) as count FROM shop_devices WHERE shop_id = %s
            ''', (shop_id,))
            existing_count = cursor.fetchone()['count']
            if existing_count > 0:
                return jsonify({'error': 'Authentication required. Please provide X-App-Id header from an active device.'}), 401
        
        cursor.execute('''
            SELECT COUNT(*) as count FROM shop_devices WHERE shop_id = %s
        ''', (shop_id,))
        device_count = cursor.fetchone()['count']
        
        if device_count >= 3:
            return jsonify({'error': 'Maximum number of devices (3) reached for this shop'}), 400
        
        cursor.execute('''
            SELECT MAX(device_slot) as max_slot FROM shop_devices WHERE shop_id = %s
        ''', (shop_id,))
        result = cursor.fetchone()
        next_slot = (result['max_slot'] or 0) + 1
        
        app_id = generate_id('APP_')
        device_id = generate_id('DEV_')
        current_time = get_timestamp()
        
        cursor.execute('''
            INSERT INTO shop_devices (id, app_id, shop_id, device_slot, status, registered_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        ''', (device_id, app_id, shop_id, next_slot, 'pending', current_time))
    
    return jsonify({
        'app_id': app_id,
        'shop_id': shop_id,
        'device_slot': next_slot,
        'status': 'pending',
        'message': f'New device registered as App {next_slot}. Please activate with a product key.'
    }), 201

@app.route('/api/shops/<shop_id>/devices/<app_id>/status', methods=['GET'])
def get_device_status(shop_id, app_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, app_id, device_slot, status, activated_at, expires_at, last_seen 
            FROM shop_devices WHERE app_id = %s AND shop_id = %s
        ''', (app_id, shop_id))
        device = cursor.fetchone()
        
        if not device:
            return jsonify({'error': 'Device not found'}), 404
        
        current_time = get_timestamp()
        device_dict = dict(device)
        
        if device_dict['expires_at'] and device_dict['expires_at'] < current_time:
            device_dict['expired'] = True
            device_dict['needs_renewal'] = True
        else:
            device_dict['expired'] = False
            device_dict['needs_renewal'] = False
        
        return jsonify(device_dict)

@app.route('/api/shops/<shop_id>/devices/<app_id>/renew', methods=['POST'])
def renew_device_license(shop_id, app_id):
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    product_key = data.get('product_key')
    if not product_key:
        return jsonify({'error': 'Product key is required'}), 400
    
    current_time = get_timestamp()
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT id, device_slot, status FROM shop_devices WHERE app_id = %s AND shop_id = %s
        ''', (app_id, shop_id))
        device = cursor.fetchone()
        
        if not device:
            return jsonify({'error': 'Device not found'}), 404
        
        cursor.execute('''
            SELECT id, status FROM product_keys WHERE product_key = %s
        ''', (product_key,))
        key_record = cursor.fetchone()
        
        if not key_record:
            return jsonify({'error': 'Invalid product key'}), 400
        
        if key_record['status'] == 'used':
            return jsonify({'error': 'Product key has already been used'}), 400
        
        expires_at = calculate_expiry_date(current_time)
        
        cursor.execute('''
            UPDATE product_keys SET 
                status = 'used', 
                activated_at = %s, 
                expires_at = %s, 
                shop_id = %s, 
                app_id = %s
            WHERE id = %s
        ''', (current_time, expires_at, shop_id, app_id, key_record['id']))
        
        cursor.execute('''
            UPDATE shop_devices SET 
                status = 'active', 
                product_key = %s, 
                activated_at = %s, 
                expires_at = %s,
                last_seen = %s
            WHERE app_id = %s AND shop_id = %s
        ''', (product_key, current_time, expires_at, current_time, app_id, shop_id))
    
    return jsonify({
        'message': 'License renewed successfully',
        'app_id': app_id,
        'shop_id': shop_id,
        'device_slot': device['device_slot'],
        'activated_at': current_time,
        'expires_at': expires_at,
        'status': 'active'
    })

@app.route('/api/shops/<shop_id>/devices/<app_id>/license-info', methods=['GET'])
def get_license_info(shop_id, app_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, app_id, device_slot, status, product_key, activated_at, expires_at, last_seen 
            FROM shop_devices WHERE app_id = %s AND shop_id = %s
        ''', (app_id, shop_id))
        device = cursor.fetchone()
        
        if not device:
            return jsonify({'error': 'Device not found'}), 404
        
        current_time = get_timestamp()
        device_dict = dict(device)
        
        product_key = device_dict.get('product_key')
        if product_key:
            parts = product_key.split('-')
            if len(parts) == 4:
                masked_key = f"****-****-****-{parts[3]}"
            else:
                masked_key = f"****-****-****-{product_key[-4:]}" if len(product_key) >= 4 else "****"
        else:
            masked_key = None
        
        expired = False
        days_remaining = None
        if device_dict['expires_at']:
            if device_dict['expires_at'] < current_time:
                expired = True
                days_remaining = 0
            else:
                remaining_ms = device_dict['expires_at'] - current_time
                days_remaining = int(remaining_ms / (1000 * 60 * 60 * 24))
        
        return jsonify({
            'app_id': device_dict['app_id'],
            'device_slot': device_dict['device_slot'],
            'status': device_dict['status'],
            'product_key_masked': masked_key,
            'activated_at': device_dict['activated_at'],
            'expires_at': device_dict['expires_at'],
            'expired': expired,
            'days_remaining': days_remaining,
            'whatsapp_support': '+263788539918'
        })

def require_admin(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({'error': 'Authorization required'}), 401
        
        token = auth_header.split(' ')[1]
        try:
            payload = jwt.decode(token, SECRET_KEY, algorithms=['HS256'])
            if payload.get('role') != 'admin':
                return jsonify({'error': 'Admin access required'}), 403
        except jwt.ExpiredSignatureError:
            return jsonify({'error': 'Token expired'}), 401
        except jwt.InvalidTokenError:
            return jsonify({'error': 'Invalid token'}), 401
        
        return f(*args, **kwargs)
    return decorated_function

@app.route('/api/admin/login', methods=['POST'])
def admin_login():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    email = data.get('email')
    password = data.get('password')
    
    if not email or not password:
        return jsonify({'error': 'Email and password required'}), 400
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT id, email, password_hash FROM admin_users WHERE email = %s', (email,))
        admin = cursor.fetchone()
        
        if not admin or not check_password_hash(admin['password_hash'], password):
            return jsonify({'error': 'Invalid email or password'}), 401
        
        token_payload = {
            'admin_id': admin['id'],
            'email': admin['email'],
            'role': 'admin',
            'exp': datetime.utcnow() + timedelta(hours=8)  # token valid for 8 hours
        }
        token = jwt.encode(token_payload, SECRET_KEY, algorithm='HS256')
    
    return jsonify({'token': token, 'message': 'Login successful'})

@app.route('/api/admin/product-keys', methods=['GET'])
@require_admin
def get_all_product_keys():
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT pk.*, s.name as shop_name 
            FROM product_keys pk 
            LEFT JOIN shops s ON pk.shop_id = s.id
            ORDER BY pk.created_at DESC
        ''')
        keys = [dict(row) for row in cursor.fetchall()]
        return jsonify(keys)

@app.route('/api/admin/product-keys', methods=['POST'])
@require_admin
def admin_create_product_key():
    key_id = generate_id('KEY_')
    product_key = generate_product_key()
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO product_keys (id, product_key, status, created_at)
            VALUES (%s, %s, 'unused', %s)
        ''', (key_id, product_key, get_timestamp()))
    
    return jsonify({
        'id': key_id,
        'product_key': product_key,
        'status': 'unused',
        'message': 'Product key generated successfully'
    }), 201

@app.route('/api/admin/shops', methods=['GET'])
@require_admin
def get_all_shops():
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT s.*, 
                   (SELECT COUNT(*) FROM shop_devices sd WHERE sd.shop_id = s.id) as device_count,
                   (SELECT COUNT(*) FROM items i WHERE i.shop_id = s.id) as item_count,
                   (SELECT COUNT(*) FROM sales sa WHERE sa.shop_id = s.id) as sale_count
            FROM shops s
            ORDER BY s.created_at DESC
        ''')
        shops = [dict(row) for row in cursor.fetchall()]
        return jsonify(shops)

@app.route('/api/admin/devices', methods=['GET'])
@require_admin
def get_all_devices():
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT sd.*, s.name as shop_name 
            FROM shop_devices sd 
            LEFT JOIN shops s ON sd.shop_id = s.id
            ORDER BY sd.registered_at DESC
        ''')
        devices = [dict(row) for row in cursor.fetchall()]
        return jsonify(devices)

@app.route('/api/admin/stats', methods=['GET'])
@require_admin
def get_admin_stats():
    with get_db_context() as conn:
        cursor = conn.cursor()
        current_time = get_timestamp()
        
        cursor.execute('SELECT COUNT(*) as total FROM shops')
        shop_count = cursor.fetchone()['total']
        
        cursor.execute('SELECT COUNT(*) as total FROM shop_devices')
        device_count = cursor.fetchone()['total']
        
        cursor.execute("SELECT COUNT(*) as total FROM product_keys WHERE status = 'unused'")
        unused_keys = cursor.fetchone()['total']
        
        cursor.execute("SELECT COUNT(*) as total FROM product_keys WHERE status = 'used'")
        used_keys = cursor.fetchone()['total']
        
        cursor.execute("SELECT COUNT(*) as total FROM shop_devices WHERE status = 'active'")
        active_devices = cursor.fetchone()['total']
        
        cursor.execute("""
            SELECT COUNT(*) as total FROM shops 
            WHERE payment_status = 'pending' OR payment_status IS NULL OR payment_status = 'overdue'
        """)
        unpaid_shops = cursor.fetchone()['total']
        
        cursor.execute("""
            SELECT COUNT(*) as total FROM shops 
            WHERE payment_status = 'paid'
        """)
        paid_shops = cursor.fetchone()['total']
        
        cursor.execute("""
            SELECT COUNT(*) as total FROM shop_devices 
            WHERE expires_at IS NOT NULL AND expires_at < %s
        """, (current_time,))
        expired_subscriptions = cursor.fetchone()['total']
        
        return jsonify({
            'total_shops': shop_count,
            'total_devices': device_count,
            'unused_product_keys': unused_keys,
            'used_product_keys': used_keys,
            'active_devices': active_devices,
            'unpaid_shops': unpaid_shops,
            'paid_shops': paid_shops,
            'expired_subscriptions': expired_subscriptions
        })

@app.route('/api/admin/subscriptions', methods=['GET'])
@require_admin
def get_subscriptions():
    with get_db_context() as conn:
        cursor = conn.cursor()
        current_time = get_timestamp()
        
        cursor.execute('''
            SELECT s.id, s.name, s.owner_name, s.owner_surname, s.phone_number,
                   s.subscription_start, s.subscription_end, s.last_payment_date, 
                   s.payment_status, s.created_at,
                   (SELECT MIN(sd.activated_at) FROM shop_devices sd WHERE sd.shop_id = s.id) as first_activation,
                   (SELECT MAX(sd.expires_at) FROM shop_devices sd WHERE sd.shop_id = s.id) as latest_expiry,
                   (SELECT COUNT(*) FROM shop_devices sd WHERE sd.shop_id = s.id AND sd.status = 'active') as active_devices
            FROM shops s
            ORDER BY s.created_at DESC
        ''')
        
        subscriptions = []
        for row in cursor.fetchall():
            sub = dict(row)
            first_activation = sub.get('first_activation') or sub.get('subscription_start')
            latest_expiry = sub.get('latest_expiry') or sub.get('subscription_end')
            
            if latest_expiry and latest_expiry < current_time:
                sub['status'] = 'expired'
            elif sub.get('payment_status') == 'paid':
                sub['status'] = 'active'
            elif first_activation:
                sub['status'] = 'pending_payment'
            else:
                sub['status'] = 'not_activated'
            
            sub['subscription_start'] = first_activation
            sub['subscription_end'] = latest_expiry
            subscriptions.append(sub)
        
        return jsonify(subscriptions)

@app.route('/api/admin/subscriptions/<shop_id>/mark-paid', methods=['POST'])
@require_admin
def mark_subscription_paid(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        current_time = get_timestamp()
        
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        thirty_days_ms = 30 * 24 * 60 * 60 * 1000
        new_end = current_time + thirty_days_ms
        
        cursor.execute('''
            UPDATE shops 
            SET payment_status = 'paid', 
                last_payment_date = %s,
                subscription_end = %s
            WHERE id = %s
        ''', (current_time, new_end, shop_id))
        
        cursor.execute('''
            UPDATE shop_devices 
            SET expires_at = %s, status = 'active'
            WHERE shop_id = %s
        ''', (new_end, shop_id))
        
        return jsonify({
            'message': 'Subscription marked as paid',
            'new_expiry': new_end
        })

@app.route('/api/admin/shops/<shop_id>', methods=['DELETE'])
@require_admin
def admin_delete_shop(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        cursor.execute('SELECT id FROM shops WHERE id = %s', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('DELETE FROM sync_logs WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM debts WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM sales WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM items WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM shop_devices WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM product_keys WHERE shop_id = %s', (shop_id,))
        cursor.execute('DELETE FROM shops WHERE id = %s', (shop_id,))
        
        return jsonify({'message': 'Shop and all related data deleted successfully'})

FRONTEND_DIR = os.path.join(os.path.dirname(__file__), '..', 'frontend', 'dist')

@app.route('/admin')
@app.route('/admin/')
@app.route('/admin/<path:path>')
def serve_admin(path=''):
    if path and os.path.exists(os.path.join(FRONTEND_DIR, path)):
        return send_from_directory(FRONTEND_DIR, path)
    return send_from_directory(FRONTEND_DIR, 'index.html')

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=True)
