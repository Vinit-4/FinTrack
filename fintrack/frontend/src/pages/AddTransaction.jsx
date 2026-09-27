import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Message from '../components/Message'
import client, { readError } from '../api/client'
import { prettyLabel, todayIso } from '../utils/format'

/** Handles both "new" and "edit": the presence of an :id in the URL decides which. */
export default function AddTransaction() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState({
    type: 'EXPENSE',
    amount: '',
    category: 'FOOD',
    description: '',
    transactionDate: todayIso(),
  })
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    client.get('/categories').then(({ data }) => setCategories(data)).catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    if (!isEditing) return
    client
      .get(`/transactions/${id}`)
      .then(({ data }) =>
        setForm({
          type: data.type,
          amount: String(data.amount),
          category: data.category,
          description: data.description || '',
          transactionDate: data.transactionDate,
        })
      )
      .catch((err) => setError(readError(err, 'Could not load this transaction')))
  }, [id, isEditing])

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (Number(form.amount) <= 0) {
      setError('Amount must be greater than zero')
      return
    }

    const payload = { ...form, amount: Number(form.amount) }
    setSubmitting(true)
    try {
      if (isEditing) {
        await client.put(`/transactions/${id}`, payload)
      } else {
        await client.post('/transactions', payload)
      }
      navigate('/transactions', { replace: true })
    } catch (err) {
      setError(readError(err, 'Could not save the transaction'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>{isEditing ? 'Edit transaction' : 'Add transaction'}</h1>
          <p className="page-sub">Record what came in or went out.</p>
        </div>
      </div>

      <form className="card form-card" onSubmit={handleSubmit}>
        <div className="type-toggle">
          {['EXPENSE', 'INCOME'].map((option) => (
            <button
              key={option}
              type="button"
              className={`toggle ${form.type === option ? 'toggle-active' : ''}`}
              onClick={() => setForm({ ...form, type: option })}
            >
              {prettyLabel(option)}
            </button>
          ))}
        </div>

        <label>
          Amount
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={form.amount}
            onChange={update('amount')}
            required
            placeholder="0.00"
          />
        </label>

        <label>
          Category
          <select value={form.category} onChange={update('category')}>
            {(categories.length ? categories : [form.category]).map((category) => (
              <option key={category} value={category}>
                {prettyLabel(category)}
              </option>
            ))}
          </select>
        </label>

        <label>
          Date
          <input type="date" value={form.transactionDate} onChange={update('transactionDate')} required />
        </label>

        <label>
          Description
          <input
            type="text"
            maxLength={255}
            value={form.description}
            onChange={update('description')}
            placeholder="Optional note, e.g. weekly groceries"
          />
        </label>

        <Message>{error}</Message>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : isEditing ? 'Save changes' : 'Add transaction'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>
            Cancel
          </button>
        </div>
      </form>
    </>
  )
}
