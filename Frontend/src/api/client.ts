import axios from 'axios'

const STORAGE_KEY = 'supplysense.auth'

// Reads the token straight from localStorage rather than importing the
// Redux store here. Importing the store into the API client (and the
// store's authSlice importing the API client, for login calls) would be
// a circular dependency - reading the same localStorage key authSlice
// already persists to sidesteps that cleanly.
function getAccessToken(): string | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw).accessToken ?? null
  } catch {
    return null
  }
}

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // The access token is missing, expired, or invalid. M7b does not
      // implement silent refresh-token renewal yet - that's a
      // reasonable next step, but for now a 401 just logs the user out
      // and sends them back to login. Cleared imperatively here (not
      // through a Redux action) because this interceptor runs outside
      // any component and doesn't have access to `dispatch`.
      localStorage.removeItem(STORAGE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
