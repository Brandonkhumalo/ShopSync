import os
import uuid
import time
from flask import Flask, request, jsonify
from database import init_db, get_db_context

app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False

def generate_id(prefix=''):
    return f"{prefix}{uuid.uuid4().hex[:12]}"

def get_timestamp():
    return int(time.time() * 1000)

@app.before_request
def before_request():
    init_db()

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
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO shops (id, name, owner_name, owner_surname, phone_number, services, address)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (
            shop_id,
            data.get('name'),
            data.get('owner_name'),
            data.get('owner_surname'),
            data.get('phone_number'),
            data.get('services', ''),
            data.get('address', '')
        ))
    
    return jsonify({'id': shop_id, 'message': 'Shop registered successfully'}), 201

@app.route('/api/shops/<shop_id>', methods=['GET'])
def get_shop(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM shops WHERE id = ?', (shop_id,))
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
        cursor.execute('SELECT id FROM shops WHERE id = ?', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            UPDATE shops SET
                name = COALESCE(?, name),
                owner_name = COALESCE(?, owner_name),
                owner_surname = COALESCE(?, owner_surname),
                phone_number = COALESCE(?, phone_number),
                services = COALESCE(?, services),
                address = COALESCE(?, address),
                updated_at = ?
            WHERE id = ?
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
            cursor.execute('SELECT * FROM items WHERE shop_id = ? AND category = ?', (shop_id, category))
        else:
            cursor.execute('SELECT * FROM items WHERE shop_id = ?', (shop_id,))
        
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
        cursor.execute('SELECT id FROM shops WHERE id = ?', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO items (id, local_id, shop_id, name, category, price_usd, price_zwg, quantity, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        cursor.execute('SELECT id FROM items WHERE (id = ? OR local_id = ?) AND shop_id = ?', (item_id, item_id, shop_id))
        if not cursor.fetchone():
            return jsonify({'error': 'Item not found'}), 404
        
        cursor.execute('''
            UPDATE items SET
                name = COALESCE(?, name),
                category = COALESCE(?, category),
                price_usd = COALESCE(?, price_usd),
                price_zwg = COALESCE(?, price_zwg),
                quantity = COALESCE(?, quantity),
                updated_at = ?
            WHERE (id = ? OR local_id = ?) AND shop_id = ?
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
        cursor.execute('DELETE FROM items WHERE (id = ? OR local_id = ?) AND shop_id = ?', (item_id, item_id, shop_id))
        if cursor.rowcount == 0:
            return jsonify({'error': 'Item not found'}), 404
    
    return jsonify({'message': 'Item deleted successfully'})

@app.route('/api/shops/<shop_id>/categories', methods=['GET'])
def get_categories(shop_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT DISTINCT category FROM items WHERE shop_id = ? AND category IS NOT NULL', (shop_id,))
        categories = [row['category'] for row in cursor.fetchall()]
        return jsonify(categories)

@app.route('/api/shops/<shop_id>/sales', methods=['GET'])
def get_sales(shop_id):
    start_date = request.args.get('start_date', type=int)
    end_date = request.args.get('end_date', type=int)
    
    with get_db_context() as conn:
        cursor = conn.cursor()
        
        query = 'SELECT * FROM sales WHERE shop_id = ?'
        params = [shop_id]
        
        if start_date:
            query += ' AND sale_date >= ?'
            params.append(start_date)
        if end_date:
            query += ' AND sale_date <= ?'
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
        cursor.execute('SELECT id FROM shops WHERE id = ?', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO sales (id, local_id, shop_id, item_id, item_name, quantity, total_usd, total_zwg,
                             payment_method, debt_used_usd, debt_used_zwg, debt_id, sale_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            FROM sales WHERE shop_id = ?
        '''
        params = [shop_id]
        
        if start_date:
            query += ' AND sale_date >= ?'
            params.append(start_date)
        if end_date:
            query += ' AND sale_date <= ?'
            params.append(end_date)
        
        cursor.execute(query, params)
        summary = dict(cursor.fetchone())
        
        top_items_query = '''
            SELECT item_name, SUM(quantity) as total_qty, SUM(total_usd) as revenue_usd
            FROM sales WHERE shop_id = ?
        '''
        top_params = [shop_id]
        
        if start_date:
            top_items_query += ' AND sale_date >= ?'
            top_params.append(start_date)
        if end_date:
            top_items_query += ' AND sale_date <= ?'
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
            cursor.execute('SELECT * FROM debts WHERE shop_id = ? ORDER BY created_at DESC', (shop_id,))
        else:
            cursor.execute('SELECT * FROM debts WHERE shop_id = ? AND cleared = 0 ORDER BY created_at DESC', (shop_id,))
        
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
        cursor.execute('SELECT id FROM shops WHERE id = ?', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        cursor.execute('''
            INSERT INTO debts (id, local_id, shop_id, customer_name, amount_usd, amount_zwg, type, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        cursor.execute('SELECT id FROM debts WHERE (id = ? OR local_id = ?) AND shop_id = ?', (debt_id, debt_id, shop_id))
        if not cursor.fetchone():
            return jsonify({'error': 'Debt not found'}), 404
        
        cursor.execute('''
            UPDATE debts SET
                customer_name = COALESCE(?, customer_name),
                amount_usd = COALESCE(?, amount_usd),
                amount_zwg = COALESCE(?, amount_zwg),
                type = COALESCE(?, type),
                notes = COALESCE(?, notes),
                updated_at = ?
            WHERE (id = ? OR local_id = ?) AND shop_id = ?
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
            UPDATE debts SET cleared = 1, cleared_at = ?, updated_at = ?
            WHERE (id = ? OR local_id = ?) AND shop_id = ?
        ''', (get_timestamp(), get_timestamp(), debt_id, debt_id, shop_id))
        
        if cursor.rowcount == 0:
            return jsonify({'error': 'Debt not found'}), 404
    
    return jsonify({'message': 'Debt cleared successfully'})

@app.route('/api/shops/<shop_id>/debts/<debt_id>', methods=['DELETE'])
def delete_debt(shop_id, debt_id):
    with get_db_context() as conn:
        cursor = conn.cursor()
        cursor.execute('DELETE FROM debts WHERE (id = ? OR local_id = ?) AND shop_id = ?', (debt_id, debt_id, shop_id))
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
            FROM debts WHERE shop_id = ?
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
        
        cursor.execute('SELECT id FROM shops WHERE id = ?', (shop_id,))
        if not cursor.fetchone():
            return jsonify({'error': 'Shop not found'}), 404
        
        for item in data.get('items', []):
            local_id = item.get('local_id')
            cursor.execute('SELECT id FROM items WHERE local_id = ? AND shop_id = ?', (local_id, shop_id))
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute('''
                    UPDATE items SET name = ?, category = ?, price_usd = ?, price_zwg = ?, quantity = ?, updated_at = ?
                    WHERE local_id = ? AND shop_id = ?
                ''', (item.get('name'), item.get('category'), item.get('price_usd', 0), 
                      item.get('price_zwg', 0), item.get('quantity', 0), get_timestamp(), local_id, shop_id))
                results['items']['updated'] += 1
            else:
                item_id = generate_id('ITEM_')
                cursor.execute('''
                    INSERT INTO items (id, local_id, shop_id, name, category, price_usd, price_zwg, quantity, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (item_id, local_id, shop_id, item.get('name'), item.get('category'),
                      item.get('price_usd', 0), item.get('price_zwg', 0), item.get('quantity', 0),
                      item.get('created_at', get_timestamp())))
                results['items']['created'] += 1
        
        for sale in data.get('sales', []):
            local_id = sale.get('local_id')
            cursor.execute('SELECT id FROM sales WHERE local_id = ? AND shop_id = ?', (local_id, shop_id))
            if not cursor.fetchone():
                sale_id = generate_id('SALE_')
                cursor.execute('''
                    INSERT INTO sales (id, local_id, shop_id, item_id, item_name, quantity, total_usd, total_zwg,
                                      payment_method, debt_used_usd, debt_used_zwg, debt_id, sale_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (sale_id, local_id, shop_id, sale.get('item_id'), sale.get('item_name'),
                      sale.get('quantity', 0), sale.get('total_usd', 0), sale.get('total_zwg', 0),
                      sale.get('payment_method', 'CASH'), sale.get('debt_used_usd', 0),
                      sale.get('debt_used_zwg', 0), sale.get('debt_id'), sale.get('sale_date', get_timestamp())))
                results['sales']['created'] += 1
        
        for debt in data.get('debts', []):
            local_id = debt.get('local_id')
            cursor.execute('SELECT id FROM debts WHERE local_id = ? AND shop_id = ?', (local_id, shop_id))
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute('''
                    UPDATE debts SET customer_name = ?, amount_usd = ?, amount_zwg = ?, type = ?, 
                                    notes = ?, cleared = ?, cleared_at = ?, updated_at = ?
                    WHERE local_id = ? AND shop_id = ?
                ''', (debt.get('customer_name'), debt.get('amount_usd', 0), debt.get('amount_zwg', 0),
                      debt.get('type', 'CREDIT_USED'), debt.get('notes', ''), 
                      1 if debt.get('cleared') else 0, debt.get('cleared_at'), get_timestamp(), local_id, shop_id))
                results['debts']['updated'] += 1
            else:
                debt_id = generate_id('DEBT_')
                cursor.execute('''
                    INSERT INTO debts (id, local_id, shop_id, customer_name, amount_usd, amount_zwg, type, notes, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (debt_id, local_id, shop_id, debt.get('customer_name'), debt.get('amount_usd', 0),
                      debt.get('amount_zwg', 0), debt.get('type', 'CREDIT_USED'), debt.get('notes', ''),
                      debt.get('created_at', get_timestamp())))
                results['debts']['created'] += 1
        
        cursor.execute('INSERT INTO sync_logs (shop_id, success) VALUES (?, 1)', (shop_id,))
    
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
            WHERE shop_id = ? ORDER BY sync_time DESC LIMIT 1
        ''', (shop_id,))
        last_sync = cursor.fetchone()
        
        if last_sync:
            return jsonify({
                'last_sync': last_sync['sync_time'],
                'success': bool(last_sync['success'])
            })
        else:
            return jsonify({'last_sync': None, 'success': None})

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=True)
