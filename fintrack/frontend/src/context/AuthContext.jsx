import { createContext, useContext, useMemo, useState } from 'react'
import client, { TOKEN_KEY, USER_KEY } from '../api/client'

const AuthContext = createContext(null)

/**
 * Holds "who is logged in" for the whole app.
 * The JWT is kept in localStorage so a page refresh does not log the user out.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem(USER_KEY)
    return stored ? JSON.parse(stored) : null
  })

  const persist = (data) => {
    const profile = { id: data.userId, name: data.name, email: data.email }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    setUser(profile)
  }

  const login = async (email, password) => {
    const { data } = await client.post('/auth/login', { email, password })
    persist(data)
  }

  const register = async (name, email, password) => {
    const { data } = await client.post('/auth/register', { name, email, password })
    persist(data)
  }

  /**
   * Logout is client-side: a JWT is stateless, so the server keeps no session
   * to destroy. Deleting the token means the browser can no longer prove who it is.
   */
  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }

  const value = useMemo(
    () => ({ user, isLoggedIn: Boolean(user), login, register, logout }),
    [user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
