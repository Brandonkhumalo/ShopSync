import { useState } from 'react'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import './App.css'

function App() {
  const [token, setToken] = useState(localStorage.getItem('adminToken'))
  const [email, setEmail] = useState(localStorage.getItem('adminEmail'))

  const handleLogin = (newToken, newEmail) => {
    setToken(newToken)
    setEmail(newEmail)
    localStorage.setItem('adminToken', newToken)
    localStorage.setItem('adminEmail', newEmail)
  }

  const handleLogout = () => {
    setToken(null)
    setEmail(null)
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminEmail')
  }

  if (!token) {
    return <Login onLogin={handleLogin} />
  }

  return <Dashboard token={token} email={email} onLogout={handleLogout} />
}

export default App
