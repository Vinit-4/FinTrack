import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Message from '../components/Message'
import { readError } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (form.password.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }

    setSubmitting(true)
    try {
      await register(form.name, form.email, form.password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(readError(err, 'Could not create the account'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-layout">
      <section className="auth-intro">
        <p className="wordmark wordmark-large">FinTrack</p>
        <h1>Start tracking in under a minute.</h1>
        <p className="auth-lede">
          Your data stays yours: every transaction is tied to your account and no one
          else can read it.
        </p>
      </section>

      <form className="card auth-card" onSubmit={handleSubmit}>
        <h2>Create your account</h2>

        <label>
          Name
          <input type="text" value={form.name} onChange={update('name')} required maxLength={100} />
        </label>

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
            minLength={6}
            autoComplete="new-password"
          />
        </label>

        <Message>{error}</Message>

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  )
}
