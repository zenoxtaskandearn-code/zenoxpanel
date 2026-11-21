import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

let accessToken = localStorage.getItem('accessToken')
let refreshToken = localStorage.getItem('refreshToken')
let isRefreshing = false
let queue = []

export function setTokens(access, refresh) {
  accessToken = access || null
  refreshToken = refresh || null
  if (access) localStorage.setItem('accessToken', access)
  else localStorage.removeItem('accessToken')
  if (refresh) localStorage.setItem('refreshToken', refresh)
  else localStorage.removeItem('refreshToken')
}

export function getAccessToken() { return accessToken || localStorage.getItem('accessToken') }
export function getRefreshToken() { return refreshToken || localStorage.getItem('refreshToken') }
export function clearTokens() { setTokens(null, null) }

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const processQueue = (error, token = null) => {
  queue.forEach(p => (error ? p.reject(error) : p.resolve(token)))
  queue = []
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error?.response?.status
    if (status === 401 && !original._retry && getRefreshToken() && !original.url?.includes('/api/auth/login')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          queue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return api(original)
        })
      }
      original._retry = true
      isRefreshing = true
      try {
        const resp = await axios.post(`${API_BASE_URL}/api/auth/refresh`, { refreshToken: getRefreshToken() })
        const data = resp.data
        setTokens(data.accessToken, data.refreshToken)
        processQueue(null, data.accessToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch (e) {
        processQueue(e, null)
        clearTokens()
        if (window.location.pathname !== '/login') window.location.href = '/login'
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default api
