import axios from 'axios'

/**
 * One axios instance for the whole app.
 *
 * Request interceptor  : attaches "Authorization: Bearer <token>" to every call,
 *                        so no page has to remember to do it.
 * Response interceptor : if the backend says 401, the token is missing or expired,
 *                        so we clear it and send the user back to the login page.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

export const TOKEN_KEY = 'fintrack.token'
export const USER_KEY = 'fintrack.user'

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/')) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      window.location.assign('/login')
    }
    return Promise.reject(error)
  }
)

/** Turns any backend error into a single readable sentence. */
export function readError(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  if (!data) return fallback
  if (data.fieldErrors) {
    const first = Object.values(data.fieldErrors)[0]
    if (first) return first
  }
  return data.message || fallback
}

export default client
