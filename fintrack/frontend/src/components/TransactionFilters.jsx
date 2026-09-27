import { prettyLabel } from '../utils/format'

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

/** Controlled filter bar. The parent owns the state and refetches on Apply. */
export default function TransactionFilters({ filters, categories, onChange, onApply, onReset }) {
  const update = (field) => (event) => onChange({ ...filters, [field]: event.target.value })

  return (
    <form
      className="filters card"
      onSubmit={(event) => {
        event.preventDefault()
        onApply()
      }}
    >
      <label>
        Type
        <select value={filters.type} onChange={update('type')}>
          <option value="">All</option>
          <option value="INCOME">Income</option>
          <option value="EXPENSE">Expense</option>
        </select>
      </label>

      <label>
        Category
        <select value={filters.category} onChange={update('category')}>
          <option value="">All</option>
          {categories.map((category) => (
            <option key={category} value={category}>
              {prettyLabel(category)}
            </option>
          ))}
        </select>
      </label>

      <label>
        From
        <input type="date" value={filters.startDate} onChange={update('startDate')} />
      </label>

      <label>
        To
        <input type="date" value={filters.endDate} onChange={update('endDate')} />
      </label>

      <label>
        Month
        <select value={filters.month} onChange={update('month')}>
          <option value="">Any</option>
          {MONTHS.map((name, index) => (
            <option key={name} value={index + 1}>
              {name}
            </option>
          ))}
        </select>
      </label>

      <label>
        Year
        <input
          type="number"
          placeholder="2026"
          min="1970"
          max="2999"
          value={filters.year}
          onChange={update('year')}
        />
      </label>

      <label>
        Sort by date
        <select value={filters.sort} onChange={update('sort')}>
          <option value="desc">Newest first</option>
          <option value="asc">Oldest first</option>
        </select>
      </label>

      <div className="filter-actions">
        <button type="submit" className="btn btn-primary">Apply filters</button>
        <button type="button" className="btn btn-ghost" onClick={onReset}>Clear</button>
      </div>
    </form>
  )
}
