/** Small inline banner for errors and confirmations. */
export default function Message({ tone = 'error', children }) {
  if (!children) return null
  return <p className={`message message-${tone}`}>{children}</p>
}
