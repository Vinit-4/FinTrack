import { useEffect, useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import Message from '../components/Message'
import client, { readError } from '../api/client'
import { formatMoney, prettyLabel } from '../utils/format'

const SLICE_COLORS = ['#1F6F5C', '#B8432F', '#C08A2E', '#3C6E92', '#7A5EA8', '#5C8A3A', '#A8545E', '#4B6357']
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

export default function Analytics() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)

  const [summary, setSummary] = useState(null)
  const [points, setPoints] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)

    Promise.all([
      client.get('/analytics/monthly-summary', { params: { year, month } }),
      client.get('/analytics/monthly', { params: { months: 12 } }),
    ])
      .then(([summaryRes, monthlyRes]) => {
        if (!active) return
        setSummary(summaryRes.data)
        setPoints(monthlyRes.data)
        setError('')
      })
      .catch((err) => active && setError(readError(err, 'Could not load analytics')))
      .finally(() => active && setLoading(false))

    return () => {
      active = false
    }
  }, [year, month])

  const pieData = (summary?.categoryTotals || []).map((item) => ({
    name: prettyLabel(item.category),
    value: Number(item.total),
  }))

  const seriesData = points.map((point) => ({
    name: point.label,
    Income: Number(point.income),
    Expense: Number(point.expense),
  }))

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Analytics</h1>
          <p className="page-sub">Every figure below is calculated by the backend from your transactions.</p>
        </div>

        <div className="month-picker">
          <select value={month} onChange={(event) => setMonth(Number(event.target.value))}>
            {MONTHS.map((name, index) => (
              <option key={name} value={index + 1}>{name}</option>
            ))}
          </select>
          <input
            type="number"
            min="1970"
            max="2999"
            value={year}
            onChange={(event) => setYear(Number(event.target.value))}
          />
        </div>
      </div>

      <Message>{error}</Message>

      {loading && <p className="empty">Loading analytics…</p>}

      {!loading && summary && (
        <>
          <section className="card month-strip">
            <div>
              <p className="summary-label">{summary.monthLabel} income</p>
              <p className="summary-amount tone-income">{formatMoney(summary.totalIncome)}</p>
            </div>
            <div>
              <p className="summary-label">Expenses</p>
              <p className="summary-amount tone-expense">{formatMoney(summary.totalExpense)}</p>
            </div>
            <div>
              <p className="summary-label">Balance</p>
              <p className="summary-amount tone-balance">{formatMoney(summary.balance)}</p>
            </div>
            <div>
              <p className="summary-label">Transactions</p>
              <p className="summary-amount tone-neutral mono">{summary.transactionCount}</p>
            </div>
          </section>

          <section className="chart-grid">
            <article className="card">
              <h2>Spending by category</h2>
              {pieData.length === 0 ? (
                <p className="empty">No expenses in {summary.monthLabel}.</p>
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={65} outerRadius={110} paddingAngle={2}>
                      {pieData.map((entry, index) => (
                        <Cell key={entry.name} fill={SLICE_COLORS[index % SLICE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatMoney(value)} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </article>

            <article className="card">
              <h2>Income vs expenses, last 12 months</h2>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={seriesData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" fontSize={11} interval={0} angle={-35} textAnchor="end" height={60} />
                  <YAxis fontSize={12} width={70} />
                  <Tooltip formatter={(value) => formatMoney(value)} />
                  <Legend />
                  <Bar dataKey="Income" fill="#1F6F5C" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Expense" fill="#B8432F" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </article>
          </section>

          <section className="card">
            <h2>Spending trend</h2>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={seriesData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" fontSize={11} />
                <YAxis fontSize={12} width={70} />
                <Tooltip formatter={(value) => formatMoney(value)} />
                <Line type="monotone" dataKey="Expense" stroke="#B8432F" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </section>
        </>
      )}
    </>
  )
}
