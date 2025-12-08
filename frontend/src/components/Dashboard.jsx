import { useState, useEffect } from 'react'
import { getStats, getProductKeys, getShops, getDevices, generateProductKey } from '../api'

function Dashboard({ token, email, onLogout }) {
  const [activeTab, setActiveTab] = useState('overview')
  const [stats, setStats] = useState(null)
  const [productKeys, setProductKeys] = useState([])
  const [shops, setShops] = useState([])
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [newKey, setNewKey] = useState(null)

  useEffect(() => {
    loadData()
  }, [token])

  const loadData = async () => {
    setLoading(true)
    try {
      const [statsData, keysData, shopsData, devicesData] = await Promise.all([
        getStats(token),
        getProductKeys(token),
        getShops(token),
        getDevices(token)
      ])
      setStats(statsData)
      setProductKeys(keysData)
      setShops(shopsData)
      setDevices(devicesData)
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

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    alert('Copied to clipboard!')
  }

  const formatDate = (timestamp) => {
    if (!timestamp) return '-'
    return new Date(timestamp).toLocaleString()
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
                
                {shops.length === 0 ? (
                  <div className="data-table">
                    <div className="empty-state">
                      <h3>No shops registered yet</h3>
                      <p>Shops will appear here once users register</p>
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
                        </tr>
                      </thead>
                      <tbody>
                        {shops.map(shop => (
                          <tr key={shop.id}>
                            <td><strong>{shop.name}</strong></td>
                            <td>{shop.owner_name} {shop.owner_surname}</td>
                            <td>{shop.phone_number}</td>
                            <td>{shop.device_count}</td>
                            <td>{shop.item_count}</td>
                            <td>{shop.sale_count}</td>
                            <td>{formatDate(shop.created_at)}</td>
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
