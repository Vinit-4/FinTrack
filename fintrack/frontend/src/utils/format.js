const currencyFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
})

export function formatMoney(value) {
  const amount = Number(value ?? 0)
  return currencyFormatter.format(Number.isFinite(amount) ? amount : 0)
}

export function formatDate(isoDate) {
  if (!isoDate) return ''
  return new Date(isoDate).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/** FOOD -> Food, ENTERTAINMENT -> Entertainment */
export function prettyLabel(value) {
  if (!value) return ''
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10)
}
