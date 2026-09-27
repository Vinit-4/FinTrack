import { formatMoney } from '../utils/format'

/** One financial metric. `tone` colours the figure: income, expense or balance. */
export default function SummaryCard({ label, amount, tone = 'neutral', caption }) {
  return (
    <article className="card summary-card">
      <p className="summary-label">{label}</p>
      <p className={`summary-amount tone-${tone}`}>{formatMoney(amount)}</p>
      {caption && <p className="summary-caption">{caption}</p>}
    </article>
  )
}
