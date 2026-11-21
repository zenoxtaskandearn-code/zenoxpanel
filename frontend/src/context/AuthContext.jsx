import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api, { setTokens, clearTokens, getAccessToken, getRefreshToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('adminProfile')
    return raw ? JSON.parse(raw) : null
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const init = async () => {
      const token = getAccessToken()
      if (!token) { setLoading(false); return }
      try {
        const res = await api.get('/api/auth/me')
        setUser(res.data)
        localStorage.setItem('adminProfile', JSON.stringify(res.data))
      } catch {
        clearTokens()
        localStorage.removeItem('adminProfile')
        setUser(null)
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [])

  const login = async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password })
    setTokens(res.data.accessToken, res.data.refreshToken)
    setUser(res.data.admin)
    localStorage.setItem('adminProfile', JSON.stringify(res.data.admin))
    return res.data
  }

  const register = async (payload) => {
    const res = await api.post('/api/auth/register', payload)
    setTokens(res.data.accessToken, res.data.refreshToken)
    setUser(res.data.admin)
    localStorage.setItem('adminProfile', JSON.stringify(res.data.admin))
    return res.data
  }

  const logout = async () => {
    try { await api.post('/api/auth/logout', { refreshToken: getRefreshToken() }) } catch {}
    clearTokens()
    localStorage.removeItem('adminProfile')
    setUser(null)
  }

  const refreshProfile = async () => {
    const res = await api.get('/api/auth/me')
    setUser(res.data)
    localStorage.setItem('adminProfile', JSON.stringify(res.data))
    return res.data
  }

  const value = useMemo(() => ({ user, loading, login, register, logout, refreshProfile, isAuthenticated: !!user }), [user, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
