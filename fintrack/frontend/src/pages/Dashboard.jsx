import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import Message from '../components/Message'
import SummaryCard from '../components/SummaryCard'
import TransactionTable from '../components/TransactionTable'
import client, { readError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { formatMoney, prettyLabel } from '../utils/format'

const SLICE_COLORS = ['#1F6F5C', '#B8432F', '#C08A2E', '#3C6E92', '#7A5EA8', '#5C8A3A', '#A8545E', '#4B6357']

export default function Dashboard() {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [categoryTotals, setCategoryTotals] = useState([])
  const [monthlyPoints, setMonthlyPoints] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true

    async function load() {
      try {
        // Three independent calls, fired together instead of one after another.
        const [summaryRes, categoriesRes, monthlyRes] = await Promise.all([
          client.get('/dashboard/summary'),
          client.get('/analytics/categories'),
          client.get('/analytics/monthly', { params: { months: 6 } }),
        ])
        if (!active) return
        setSummary(summaryRes.data)
        setCategoryTotals(categoriesRes.data)
        setMonthlyPoints(monthlyRes.data)
      } catch (err) {
        if (active) setError(readError(err, 'Could not load your dashboard'))
      } finally {
        if (active) setLoading(false)
      }
    }

    load()
    return () => {
      active = false
    }
  }, [])

  if (loading) return <p className="empty">Loading your dashboard…</p>
  if (error) return <Message>{error}</Message>

  const pieData = categoryTotals.map((item) => ({
    name: prettyLabel(item.category),
    value: Number(item.total),
  }))

  const barData = monthlyPoints.map((point) => ({
    name: point.label,
    Income: Number(point.income),
    Expense: Number(point.expense),
  }))

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Hello, {user?.name?.split(' ')[0]}</h1>
          <p className="page-sub">Here is where your money stands today.</p>
        </div>
        <Link className="btn btn-primary" to="/transactions/new">Add transaction</Link>
      </div>

      <section className="card-grid">
        <SummaryCard label="Current balance" amount={summary.balance} tone="balance" caption="Income minus expenses, all time" />
        <SummaryCard label="Total income" amount={summary.totalIncome} tone="income" />
        <SummaryCard label="Total expenses" amount={summary.totalExpense} tone="expense" />
        <SummaryCard label="This month" amount={summary.currentMonthBalance} tone="balance"
          caption={`${formatMoney(summary.currentMonthIncome)} in · ${formatMoney(summary.currentMonthExpense)} out`} />
      </section>

      <section className="chart-grid">
        <article className="card">
          <h2>Where this month went</h2>
          {pieData.length === 0 ? (
            <p className="empty">No expenses recorded this month yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={100} paddingAngle={2}>
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
          <h2>Income vs expenses</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" fontSize={12} />
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
        <div className="section-head">
          <h2>Recent transactions</h2>
          <Link to="/transactions">See all</Link>
        </div>
        <TransactionTable transactions={summary.recentTransactions} showActions={false} />
      </section>
    </>
  )
}
