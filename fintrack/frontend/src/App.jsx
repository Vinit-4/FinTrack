import { Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import AddTransaction from './pages/AddTransaction'
import Analytics from './pages/Analytics'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import Transactions from './pages/Transactions'
import { useAuth } from './context/AuthContext'

export default function App() {
  const { isLoggedIn } = useAuth()

  return (
    <>
      {isLoggedIn && <Navbar />}
      <main className="page">
        <Routes>
          <Route path="/login" element={isLoggedIn ? <Navigate to="/" replace /> : <Login />} />
          <Route path="/register" element={isLoggedIn ? <Navigate to="/" replace /> : <Register />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/transactions" element={<Transactions />} />
            <Route path="/transactions/new" element={<AddTransaction />} />
            <Route path="/transactions/:id/edit" element={<AddTransaction />} />
            <Route path="/analytics" element={<Analytics />} />
          </Route>

          <Route path="*" element={<Navigate to={isLoggedIn ? '/' : '/login'} replace />} />
        </Routes>
      </main>
    </>
  )
}
