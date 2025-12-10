import { useState, useEffect } from 'react'
import { getStats, getProductKeys, getShops, getDevices, generateProductKey, getSubscriptions, markSubscriptionPaid, deleteShop } from '../api'

function Dashboard({ token, email, onLogout }) {
  const [activeTab, setActiveTab] = useState('overview')
  const [stats, setStats] = useState(null)
  const [productKeys, setProductKeys] = useState([])
  const [shops, setShops] = useState([])
  const [devices, setDevices] = useState([])
  const [subscriptions, setSubscriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [newKey, setNewKey] = useState(null)
  const [markingPaid, setMarkingPaid] = useState(null)
  const [shopSearch, setShopSearch] = useState('')
  const [deletingShop, setDeletingShop] = useState(null)

  useEffect(() => {
    loadData()
  }, [token])

  const loadData = async () => {
    setLoading(true)
    try {
      const [statsData, keysData, shopsData, devicesData, subsData] = await Promise.all([
        getStats(token),
        getProductKeys(token),
        getShops(token),
        getDevices(token),
        getSubscriptions(token)
      ])
      setStats(statsData)
      setProductKeys(keysData)
      setShops(shopsData)
      setDevices(devicesData)
      setSubscriptions(subsData)
    } catch (err) {
      console.error('Failed to load data:', err)
      if (err.message.includes('401') || err.message.includes('expired')) {
        onLogout()
      }
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateKey = async () => {
    setGenerating(true)
    try {
      const data = await generateProductKey(token)
      setNewKey(data.product_key)
      loadData()
    } catch (err) {
      alert('Failed to generate key: ' + err.message)
    } finally {
      setGenerating(false)
    }
  }

  const handleMarkPaid = async (shopId) => {
    if (!confirm('Mark this subscription as paid? This will extend their license by 30 days.')) {
      return
    }
    setMarkingPaid(shopId)
    try {
      await markSubscriptionPaid(token, shopId)
      loadData()
    } catch (err) {
      alert('Failed to mark as paid: ' + err.message)
    } finally {
      setMarkingPaid(null)
    }
  }

  const handleDeleteShop = async (shopId, shopName) => {
    if (!confirm(`Are you sure you want to delete "${shopName}" and ALL its data? This action cannot be undone!`)) {
      return
    }
    if (!confirm(`FINAL WARNING: This will permanently delete the shop, all items, sales, debts, and device registrations. Continue?`)) {
      return
    }
    setDeletingShop(shopId)
    try {
      await deleteShop(token, shopId)
      alert('Shop deleted successfully')
      loadData()
    } catch (err) {
      alert('Failed to delete shop: ' + err.message)
    } finally {
      setDeletingShop(null)
    }
  }

  const filteredShops = shops.filter(shop => {
    if (!shopSearch.trim()) return true
    const search = shopSearch.toLowerCase()
    return (
      shop.name?.toLowerCase().includes(search) ||
      shop.owner_name?.toLowerCase().includes(search) ||
      shop.owner_surname?.toLowerCase().includes(search)
    )
  })

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    alert('Copied to clipboard!')
  }

  const formatDate = (timestamp) => {
    if (!timestamp) return '-'
    return new Date(timestamp).toLocaleString()
  }

  const formatDateShort = (timestamp) => {
    if (!timestamp) return '-'
    return new Date(timestamp).toLocaleDateString()
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'active':
      case 'paid':
        return 'active'
      case 'expired':
      case 'overdue':
        return 'expired'
      case 'pending_payment':
      case 'pending':
        return 'pending'
      default:
        return 'inactive'
    }
  }

  const getDaysRemaining = (expiresAt) => {
    if (!expiresAt) return null
    const now = Date.now()
    const diff = expiresAt - now
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
    return days
  }

  return (
    <div className="dashboard">
      <header className="header">
        <h1>ShopSync Admin</h1>
        <div className="header-right">
          <span className="header-email">{email}</span>
          <button className="logout-btn" onClick={onLogout}>Logout</button>
        </div>
      </header>

      <nav className="nav-tabs">
        <button 
          className={`nav-tab ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          Overview
        </button>
        <button 
          className={`nav-tab ${activeTab === 'subscriptions' ? 'active' : ''}`}
          onClick={() => setActiveTab('subscriptions')}
        >
          Subscriptions
        </button>
        <button 
          className={`nav-tab ${activeTab === 'keys' ? 'active' : ''}`}
          onClick={() => setActiveTab('keys')}
        >
          Product Keys
        </button>
        <button 
          className={`nav-tab ${activeTab === 'shops' ? 'active' : ''}`}
          onClick={() => setActiveTab('shops')}
        >
          Shops
        </button>
        <button 
          className={`nav-tab ${activeTab === 'devices' ? 'active' : ''}`}
          onClick={() => setActiveTab('devices')}
        >
          Devices
        </button>
      </nav>

      <div className="content">
        {loading ? (
          <div className="loading">Loading...</div>
        ) : (
          <>
            {activeTab === 'overview' && stats && (
              <>
                <div className="stats-grid">
                  <div className="stat-card highlight">
                    <h3>Total Shops</h3>
                    <div className="value">{stats.total_shops}</div>
                  </div>
                  <div className="stat-card">
                    <h3>Total Devices</h3>
                    <div className="value">{stats.total_devices}</div>
                  </div>
                  <div className="stat-card">
                    <h3>Active Devices</h3>
                    <div className="value">{stats.active_devices}</div>
                  </div>
                  <div className="stat-card warning">
                    <h3>Unpaid Shops</h3>
                    <div className="value">{stats.unpaid_shops}</div>
                  </div>
                  <div className="stat-card success">
                    <h3>Paid Shops</h3>
                    <div className="value">{stats.paid_shops}</div>
                  </div>
                  <div className="stat-card danger">
                    <h3>Expired Subscriptions</h3>
                    <div className="value">{stats.expired_subscriptions}</div>
                  </div>
                  <div className="stat-card">
                    <h3>Unused Keys</h3>
                    <div className="value">{stats.unused_product_keys}</div>
                  </div>
                  <div className="stat-card">
                    <h3>Used Keys</h3>
                    <div className="value">{stats.used_product_keys}</div>
                  </div>
                </div>

                <div className="section-header">
                  <h2>Quick Actions</h2>
                </div>
                <button 
                  className="generate-btn"
                  onClick={handleGenerateKey}
                  disabled={generating}
                >
                  {generating ? 'Generating...' : 'Generate New Product Key'}
                </button>
              </>
            )}

            {activeTab === 'subscriptions' && (
              <>
                <div className="section-header">
                  <h2>Subscription Management</h2>
                  <span className="sub-header">Track payments and subscription status ($10/month per shop)</span>
                </div>
                
                {subscriptions.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No subscriptions yet</h3>
                      <p>Subscriptions will appear here when shops activate their product keys</p>
                    </div>
                  </div>
                ) : (
                  <div className="data-table">
                    <table>
                      <thead>
                        <tr>
                          <th>Shop Name</th>
                          <th>Owner</th>
                          <th>Phone</th>
                          <th>Status</th>
                          <th>Subscription Start</th>
                          <th>Subscription End</th>
                          <th>Last Payment</th>
                          <th>Days Left</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {subscriptions.map(sub => {
                          const daysLeft = getDaysRemaining(sub.subscription_end)
                          return (
                            <tr key={sub.id} className={sub.status === 'expired' || sub.status === 'pending_payment' ? 'row-warning' : ''}>
                              <td><strong>{sub.name}</strong></td>
                              <td>{sub.owner_name} {sub.owner_surname}</td>
                              <td>{sub.phone_number}</td>
                              <td>
                                <span className={`status-badge ${getStatusColor(sub.status)}`}>
                                  {sub.status === 'pending_payment' ? 'UNPAID' : sub.status.toUpperCase()}
                                </span>
                              </td>
                              <td>{formatDateShort(sub.subscription_start)}</td>
                              <td>{formatDateShort(sub.subscription_end)}</td>
                              <td>{formatDateShort(sub.last_payment_date)}</td>
                              <td>
                                {daysLeft !== null ? (
                                  <span className={daysLeft <= 5 ? 'days-warning' : daysLeft <= 0 ? 'days-expired' : ''}>
                                    {daysLeft <= 0 ? 'Expired' : `${daysLeft} days`}
                                  </span>
                                ) : '-'}
                              </td>
                              <td>
                                {(sub.status === 'pending_payment' || sub.status === 'expired' || sub.payment_status !== 'paid') && (
                                  <button 
                                    className="mark-paid-btn"
                                    onClick={() => handleMarkPaid(sub.id)}
                                    disabled={markingPaid === sub.id}
                                  >
                                    {markingPaid === sub.id ? 'Processing...' : 'Mark Paid'}
                                  </button>
                                )}
                                {sub.status === 'active' && sub.payment_status === 'paid' && (
                                  <span className="paid-label">Paid</span>
                                )}
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}

            {activeTab === 'keys' && (
              <>
                <div className="section-header">
                  <h2>Product Keys</h2>
                  <button 
                    className="generate-btn"
                    onClick={handleGenerateKey}
                    disabled={generating}
                  >
                    {generating ? 'Generating...' : 'Generate New Key'}
                  </button>
                </div>
                
                {productKeys.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No product keys yet</h3>
                      <p>Generate your first product key to get started</p>
                    </div>
                  </div>
                ) : (
                  <div className="data-table">
                    <table>
                      <thead>
                        <tr>
                          <th>Product Key</th>
                          <th>Status</th>
                          <th>Shop</th>
                          <th>Created</th>
                          <th>Activated</th>
                          <th>Expires</th>
                        </tr>
                      </thead>
                      <tbody>
                        {productKeys.map(key => (
                          <tr key={key.id}>
                            <td>
                              <span className="product-key">{key.product_key}</span>
                              <button 
                                className="copy-btn"
                                onClick={() => copyToClipboard(key.product_key)}
                              >
                                Copy
                              </button>
                            </td>
                            <td>
                              <span className={`status-badge ${key.status}`}>
                                {key.status}
                              </span>
                            </td>
                            <td>{key.shop_name || '-'}</td>
                            <td>{formatDate(key.created_at)}</td>
                            <td>{formatDate(key.activated_at)}</td>
                            <td>{formatDate(key.expires_at)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}

            {activeTab === 'shops' && (
              <>
                <div className="section-header">
                  <h2>Shops</h2>
                </div>
                
                <div className="search-bar">
                  <input
                    type="text"
                    placeholder="Search by shop name or owner name..."
                    value={shopSearch}
                    onChange={(e) => setShopSearch(e.target.value)}
                    className="search-input"
                  />
                  {shopSearch && (
                    <button className="clear-search" onClick={() => setShopSearch('')}>
                      Clear
                    </button>
                  )}
                </div>
                
                {shops.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No shops registered yet</h3>
                      <p>Shops will appear here once users register</p>
                    </div>
                  </div>
                ) : filteredShops.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No shops match your search</h3>
                      <p>Try a different search term</p>
                    </div>
                  </div>
                ) : (
                  <div className="data-table">
                    <table>
                      <thead>
                        <tr>
                          <th>Shop Name</th>
                          <th>Owner</th>
                          <th>Phone</th>
                          <th>Devices</th>
                          <th>Items</th>
                          <th>Sales</th>
                          <th>Created</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredShops.map(shop => (
                          <tr key={shop.id}>
                            <td><strong>{shop.name}</strong></td>
                            <td>{shop.owner_name} {shop.owner_surname}</td>
                            <td>{shop.phone_number}</td>
                            <td>{shop.device_count}</td>
                            <td>{shop.item_count}</td>
                            <td>{shop.sale_count}</td>
                            <td>{formatDate(shop.created_at)}</td>
                            <td>
                              <button 
                                className="delete-btn"
                                onClick={() => handleDeleteShop(shop.id, shop.name)}
                                disabled={deletingShop === shop.id}
                              >
                                {deletingShop === shop.id ? 'Deleting...' : 'Delete'}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}

            {activeTab === 'devices' && (
              <>
                <div className="section-header">
                  <h2>Devices</h2>
                </div>
                
                {devices.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No devices registered yet</h3>
                      <p>Devices will appear here once users register</p>
                    </div>
                  </div>
                ) : (
                  <div className="data-table">
                    <table>
                      <thead>
                        <tr>
                          <th>App ID</th>
                          <th>Shop</th>
                          <th>Slot</th>
                          <th>Status</th>
                          <th>Registered</th>
                          <th>Activated</th>
                          <th>Expires</th>
                          <th>Last Seen</th>
                        </tr>
                      </thead>
                      <tbody>
                        {devices.map(device => (
                          <tr key={device.id}>
                            <td><code>{device.app_id}</code></td>
                            <td>{device.shop_name || '-'}</td>
                            <td>{device.device_slot}</td>
                            <td>
                              <span className={`status-badge ${device.status}`}>
                                {device.status}
                              </span>
                            </td>
                            <td>{formatDate(device.registered_at)}</td>
                            <td>{formatDate(device.activated_at)}</td>
                            <td>{formatDate(device.expires_at)}</td>
                            <td>{formatDate(device.last_seen)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>

      {newKey && (
        <div className="new-key-modal" onClick={() => setNewKey(null)}>
          <div className="new-key-content" onClick={e => e.stopPropagation()}>
            <h2>New Product Key Generated!</h2>
            <p>Share this key with your customer:</p>
            <div className="product-key">{newKey}</div>
            <div>
              <button 
                className="copy-btn"
                onClick={() => copyToClipboard(newKey)}
              >
                Copy to Clipboard
              </button>
            </div>
            <button className="close-btn" onClick={() => setNewKey(null)}>
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default Dashboard
