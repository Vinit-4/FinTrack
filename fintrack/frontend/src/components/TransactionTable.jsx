import { Link } from 'react-router-dom'
import { formatDate, formatMoney, prettyLabel } from '../utils/format'

/**
 * Reused by the dashboard (read-only) and the transactions page (with actions).
 */
export default function TransactionTable({ transactions, onDelete, showActions = true, emptyMessage }) {
  if (!transactions.length) {
    return <p className="empty">{emptyMessage || 'No transactions yet. Add your first one to get started.'}</p>
  }

  return (
    <div className="table-scroll">
      <table className="table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Description</th>
            <th>Category</th>
            <th className="align-right">Amount</th>
            {showActions && <th className="align-right">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {transactions.map((transaction) => (
            <tr key={transaction.id}>
              <td className="mono">{formatDate(transaction.transactionDate)}</td>
              <td>{transaction.description || '—'}</td>
              <td>
                <span className="chip">{prettyLabel(transaction.category)}</span>
              </td>
              <td className={`align-right mono ${transaction.type === 'INCOME' ? 'tone-income' : 'tone-expense'}`}>
                {transaction.type === 'INCOME' ? '+' : '−'} {formatMoney(transaction.amount)}
              </td>
              {showActions && (
                <td className="align-right row-actions">
                  <Link className="btn btn-ghost btn-small" to={`/transactions/${transaction.id}/edit`}>
                    Edit
                  </Link>
                  <button
                    type="button"
                    className="btn btn-ghost btn-small btn-danger"
                    onClick={() => onDelete(transaction)}
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
