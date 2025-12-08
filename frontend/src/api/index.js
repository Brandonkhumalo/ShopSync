const API_BASE = '/api/admin'

export async function login(email, password) {
  const response = await fetch(`${API_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  })
  const data = await response.json()
  if (!response.ok) {
    throw new Error(data.error || 'Login failed')
  }
  return data
}

export async function getStats(token) {
  const response = await fetch(`${API_BASE}/stats`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!response.ok) {
    throw new Error('Failed to fetch stats')
  }
  return response.json()
}

export async function getProductKeys(token) {
  const response = await fetch(`${API_BASE}/product-keys`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!response.ok) {
    throw new Error('Failed to fetch product keys')
  }
  return response.json()
}

export async function generateProductKey(token) {
  const response = await fetch(`${API_BASE}/product-keys`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })
  if (!response.ok) {
    throw new Error('Failed to generate product key')
  }
  return response.json()
}

export async function getShops(token) {
  const response = await fetch(`${API_BASE}/shops`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!response.ok) {
    throw new Error('Failed to fetch shops')
  }
  return response.json()
}

export async function getDevices(token) {
  const response = await fetch(`${API_BASE}/devices`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!response.ok) {
    throw new Error('Failed to fetch devices')
  }
  return response.json()
}
