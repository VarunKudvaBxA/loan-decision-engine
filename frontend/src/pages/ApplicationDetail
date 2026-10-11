import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, getUser, money, pct } from '../api.js'
import ReasonBars from '../components/ReasonBars.jsx'

export default function ApplicationDetail() {
  const { id } = useParams()
  const user = getUser()
  const [data, setData] = useState(null)
  const [choice, setChoice] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    api('/applications/' + id).then(setData).catch((e) => setError(e.message))
  }, [id])

  useEffect(load, [load])

  const send = async (path) => {
    setBusy(true)
    setError('')
    try {
      await api('/applications/' + id + path, { method: 'POST', body: { decision: choice, note } })
      setChoice('')
      setNote('')
      load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  if (error && !data) return <div className="error">{error}</div>
  if (!data) return <p className="muted">Loading...</p>

  const { application: a, decision: d, audit } = data

  return (
    <>
      <div className="page-head">
        <h1>{a.applicantName}</h1>
        <p className="muted">
          Application {a.id}, {money(a.loanAmount)} over {a.termMonths} months, submitted by {a.createdBy}{' '}
          <span className={'tag ' + a.status}>{a.status.replace('_', ' ').toLowerCase()}</span>
        </p>
      </div>

      <div className="panel">
        <div className="score-line">
          <div className="score-big">{pct(d.riskScore)}</div>
          <div>
            <div>estimated chance of default</div>
            <div>System recommends <span className={'tag ' + d.systemRecommendation}>{d.systemRecommendation}</span></div>
          </div>
        </div>
        <p className="explain">{d.explanation}</p>
        <p className="muted small">
          Written by {d.explanationSource === 'LLM' ? 'an LLM from the structured result only' : 'a fixed template'}.
          Model {d.modelVersion}, {d.rulesVersion}.
        </p>
      </div>

      <div className="two-col">
        <div className="panel">
          <h2>What drove the score</h2>
          <ReasonBars reasons={d.reasonCodes} />
        </div>
        <div className="panel">
          <h2>Policy rules that fired</h2>
          {d.ruleHits.length === 0 ? (
            <p className="muted">None.</p>
          ) : (
            <table>
              <tbody>
                {d.ruleHits.map((h) => (
                  <tr key={h.ruleId}>
                    <td>
                      {h.message}
                      <div className="muted small">value {h.actual}, limit {h.threshold}</div>
                    </td>
                    <td><span className={'tag ' + h.outcome}>{h.outcome}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {a.status === 'PENDING_REVIEW' && user.role === 'ANALYST' && (
        <div className="panel">
          <h2>Your recommendation</h2>
          <div className="form-grid">
            <div>
              <label htmlFor="rec">Recommendation</label>
              <select id="rec" value={choice} onChange={(e) => setChoice(e.target.value)}>
                <option value="">Choose one</option>
                <option>APPROVE</option>
                <option>REFER</option>
                <option>DECLINE</option>
              </select>
            </div>
            <div className="wide">
              <label htmlFor="note">Note for the approver</label>
              <textarea id="note" value={note} onChange={(e) => setNote(e.target.value)} />
            </div>
          </div>
          {error && <div className="error">{error}</div>}
          <div style={{ marginTop: 12 }}>
            <button disabled={!choice || busy} onClick={() => send('/recommend')}>Send to approver</button>
          </div>
        </div>
      )}

      {a.status === 'PENDING_REVIEW' && user.role !== 'ANALYST' && (
        <p className="muted">Waiting for an analyst to review this one.</p>
      )}

      {a.status === 'PENDING_APPROVAL' && (
        <div className="panel">
          <h2>Approval</h2>
          <p>
            {d.recommendedBy} recommends <span className={'tag ' + d.analystRecommendation}>{d.analystRecommendation}</span>
            {d.analystNote ? ': ' + d.analystNote : '.'}
          </p>
          {user.role === 'SENIOR' ? (
            <>
              <div className="form-grid">
                <div>
                  <label htmlFor="fin">Final decision</label>
                  <select id="fin" value={choice} onChange={(e) => setChoice(e.target.value)}>
                    <option value="">Choose one</option>
                    <option>APPROVE</option>
                    <option>DECLINE</option>
                  </select>
                </div>
                <div className="wide">
                  <label htmlFor="fnote">Note</label>
                  <textarea id="fnote" value={note} onChange={(e) => setNote(e.target.value)} />
                </div>
              </div>
              {error && <div className="error">{error}</div>}
              <div style={{ marginTop: 12 }}>
                <button disabled={!choice || busy} onClick={() => send('/finalize')}>Record decision</button>
              </div>
            </>
          ) : (
            <p className="muted">Only a senior reviewer can make the final call.</p>
          )}
        </div>
      )}

      {d.finalDecision && (
        <div className="panel">
          <h2>Final decision</h2>
          <p>
            {d.decidedBy} decided <span className={'tag ' + d.finalDecision}>{d.finalDecision}</span>
            {d.finalNote ? ': ' + d.finalNote : '.'}
          </p>
        </div>
      )}

      <div className="panel">
        <h2>History</h2>
        <ul className="timeline">
          {audit.map((e) => (
            <li key={e.id}>
              <strong>{e.action.toLowerCase().replace('_', ' ')}</strong> by {e.actor}
              <div className="muted small">
                {new Date(e.createdAt).toLocaleString()}{e.details ? ', ' + e.details : ''}
              </div>
            </li>
          ))}
        </ul>
      </div>
    </>
  )
}
