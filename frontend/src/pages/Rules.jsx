import { useEffect, useState } from 'react'
import { api, getUser } from '../api.js'

export default function Rules() {
  const user = getUser()
  const [rules, setRules] = useState(null)
  const [audit, setAudit] = useState(null)
  const [error, setError] = useState('')
  const canEdit = user.role === 'SENIOR'

  const load = () => api('/rules').then(setRules).catch((e) => setError(e.message))
  useEffect(() => { load() }, [])

  const update = async (rule, changes) => {
    setError('')
    try {
      await api('/rules/' + rule.id, { method: 'PUT', body: changes })
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  const verify = () => api('/audit/verify').then(setAudit).catch((e) => setError(e.message))

  if (!rules) return <p className="muted">Loading rules...</p>

  return (
    <>
      <div className="page-head">
        <h1>Policy rules</h1>
        <p className="muted">
          Rules live in the database. {canEdit ? 'Edit a limit and the next application uses it.' : 'Only a senior reviewer can change them.'}
        </p>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Field</th>
              <th className="num">Limit</th>
              <th>Outcome</th>
              <th>On</th>
              <th className="num">Version</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.id}>
                <td>{r.name}</td>
                <td className="muted">{r.field} {r.operator}</td>
                <td className="num">
                  {canEdit ? (
                    <input
                      style={{ width: 90, textAlign: 'right' }}
                      type="number"
                      defaultValue={r.threshold}
                      onBlur={(e) => {
                        if (Number(e.target.value) !== Number(r.threshold)) update(r, { threshold: Number(e.target.value) })
                      }}
                    />
                  ) : r.threshold}
                </td>
                <td><span className={'tag ' + r.outcome}>{r.outcome}</span></td>
                <td>
                  <input
                    type="checkbox"
                    style={{ width: 'auto' }}
                    checked={r.active}
                    disabled={!canEdit}
                    onChange={(e) => update(r, { active: e.target.checked })}
                  />
                </td>
                <td className="num">{r.version}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="panel">
        <h2>Audit trail</h2>
        <p className="muted">Every entry stores the hash of the one before it. Check that nobody has edited history.</p>
        <button className="secondary" onClick={verify}>Verify audit chain</button>
        {audit && (
          <p style={{ marginTop: 12 }} className={audit.valid ? 'ok' : 'error'}>
            {audit.valid
              ? 'Chain is intact, ' + audit.checked + ' entries checked.'
              : 'Chain is broken at entry ' + audit.brokenAtId + '.'}
          </p>
        )}
      </div>
    </>
  )
}
