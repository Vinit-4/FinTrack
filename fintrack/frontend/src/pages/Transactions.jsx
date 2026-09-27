import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Message from '../components/Message'
import TransactionFilters from '../components/TransactionFilters'
import TransactionTable from '../components/TransactionTable'
import client, { readError } from '../api/client'
import { formatMoney } from '../utils/format'

const EMPTY_FILTERS = {
  type: '', category: '', startDate: '', endDate: '', month: '', year: '', sort: 'desc',
}

export default function Transactions() {
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [transactions, setTransactions] = useState([])
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)

  /** Empty strings are dropped so the backend treats them as "no filter". */
  const fetchTransactions = useCallback(async (activeFilters) => {
    setLoading(true)
    setError('')
    try {
      const params = Object.fromEntries(
        Object.entries(activeFilters).filter(([, value]) => value !== '' && value !== null)
      )
      const { data } = await client.get('/transactions', { params })
      setTransactions(data)
    } catch (err) {
      setError(readError(err, 'Could not load transactions'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    client.get('/categories').then(({ data }) => setCategories(data)).catch(() => setCategories([]))
    fetchTransactions(EMPTY_FILTERS)
  }, [fetchTransactions])

  const handleDelete = async (transaction) => {
    if (!window.confirm('Delete this transaction? This cannot be undone.')) return
    try {
      await client.delete(`/transactions/${transaction.id}`)
      setTransactions((current) => current.filter((item) => item.id !== transaction.id))
      setNotice('Transaction deleted.')
    } catch (err) {
      setError(readError(err, 'Could not delete the transaction'))
    }
  }

  const total = transactions.reduce(
    (sum, item) => (item.type === 'INCOME' ? sum + Number(item.amount) : sum - Number(item.amount)),
    0
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Transactions</h1>
          <p className="page-sub">
            {transactions.length} shown · net {formatMoney(total)}
          </p>
        </div>
        <Link className="btn btn-primary" to="/transactions/new">Add transaction</Link>
      </div>

      <TransactionFilters
        filters={filters}
        categories={categories}
        onChange={setFilters}
        onApply={() => fetchTransactions(filters)}
        onReset={() => {
          setFilters(EMPTY_FILTERS)
          fetchTransactions(EMPTY_FILTERS)
        }}
      />

      <Message>{error}</Message>
      <Message tone="success">{notice}</Message>

      <section className="card">
        {loading ? (
          <p className="empty">Loading transactions…</p>
        ) : (
          <TransactionTable
            transactions={transactions}
            onDelete={handleDelete}
            emptyMessage="Nothing matches these filters. Try widening the date range."
          />
        )}
      </section>
    </>
  )
}
