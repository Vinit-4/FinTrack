import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Message from '../components/Message'
import { readError } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(form.email, form.password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(readError(err, 'Invalid email or password'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-layout">
      <section className="auth-intro">
        <p className="wordmark wordmark-large">FinTrack</p>
        <h1>Know where your money went, before you wonder.</h1>
        <p className="auth-lede">
          Record income and expenses, see the month at a glance, and spot the categories
          that quietly grow.
        </p>
      </section>

      <form className="card auth-card" onSubmit={handleSubmit}>
        <h2>Log in</h2>

        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} required autoComplete="email" />
        </label>

        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={update('password')}
            required
            autoComplete="current-password"
          />
        </label>

        <Message>{error}</Message>

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log in'}
        </button>

        <p className="auth-switch">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </div>
  )
}
