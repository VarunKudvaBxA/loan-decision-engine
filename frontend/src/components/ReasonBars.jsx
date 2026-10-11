/** shows how much each factor pushed the risk up or down */
export default function ReasonBars({ reasons }) {
  if (!reasons || reasons.length === 0) return null
  const max = Math.max(...reasons.map((r) => Math.abs(r.contribution)), 0.01)
  return (
    <div>
      {reasons.map((r) => {
        const width = (Math.abs(r.contribution) / max) * 50
        const up = r.contribution >= 0
        return (
          <div className="factor" key={r.feature}>
            <div>
              {r.label}
              <div className="muted small">value {r.value}</div>
            </div>
            <div className="bar-track" title={r.direction}>
              <div className={'bar-fill ' + (up ? 'up' : 'down')} style={{ width: width + '%' }} />
            </div>
            <div className="small" style={{ textAlign: 'right' }}>
              {up ? '+' : ''}{r.contribution}
            </div>
          </div>
        )
      })}
      <p className="muted small">Bars to the right push risk up, bars to the left bring it down (log odds).</p>
    </div>
  )
}
