// Detect environment: use proxy in dev, full URL in production
const API_BASE = 'https://shopsync-qx6o.onrender.com/api/admin';  // your deployed backend

export async function login(email, password) {
  const response = await fetch(`${API_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || 'Login failed');
  }
  return data;
}

export async function getStats(token) {
  const response = await fetch(`${API_BASE}/stats`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch stats');
  }
  return response.json();
}

export async function getProductKeys(token) {
  const response = await fetch(`${API_BASE}/product-keys`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch product keys');
  }
  return response.json();
}

export async function generateProductKey(token) {
  const response = await fetch(`${API_BASE}/product-keys`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  if (!response.ok) {
    throw new Error('Failed to generate product key');
  }
  return response.json();
}

export async function getShops(token) {
  const response = await fetch(`${API_BASE}/shops`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch shops');
  }
  return response.json();
}

export async function getDevices(token) {
  const response = await fetch(`${API_BASE}/devices`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch devices');
  }
  return response.json();
}

export async function getSubscriptions(token) {
  const response = await fetch(`${API_BASE}/subscriptions`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch subscriptions');
  }
  return response.json();
}

export async function markSubscriptionPaid(token, shopId) {
  const response = await fetch(`${API_BASE}/subscriptions/${shopId}/mark-paid`, {
    method: 'POST',
    headers: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  if (!response.ok) {
    throw new Error('Failed to mark subscription as paid');
  }
  return response.json();
}
