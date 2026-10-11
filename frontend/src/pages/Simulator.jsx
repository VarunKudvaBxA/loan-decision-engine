import { useEffect, useRef, useState } from 'react'
import { api, pct } from '../api.js'
import ApplicantForm, { emptyApplicant, toPayload } from '../components/ApplicantForm.jsx'
import ReasonBars from '../components/ReasonBars.jsx'

export default function Simulator() {
  const [form, setForm] = useState(emptyApplicant)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const timer = useRef(null)

  // wait a moment after typing stops so we don't call the api on every key
  useEffect(() => {
    clearTimeout(timer.current)
    timer.current = setTimeout(() => {
      api('/simulate', { method: 'POST', body: toPayload(form) })
        .then((r) => {
          setResult(r)
          setError('')
        })
        .catch((e) => setError(e.message))
    }, 350)
    return () => clearTimeout(timer.current)
  }, [form])

  return (
    <>
      <div className="page-head">
        <h1>What-if simulator</h1>
        <p className="muted">Change any number and watch the score move. Nothing here is saved.</p>
      </div>

      <div className="two-col">
        <div className="panel">
          <ApplicantForm form={form} onChange={setForm} showName={false} />
        </div>
        <div>
          {error && <div className="error">{error}</div>}
          {result && (
            <>
              <div className="panel">
                <div className="score-line">
                  <div className="score-big">{pct(result.pd)}</div>
                  <div>
                    <div>chance of default</div>
                    <span className={'tag ' + result.recommendation}>{result.recommendation}</span>
                  </div>
                </div>
                {result.hits.map((h) => (
                  <div key={h.ruleId} className="small">
                    <span className={'tag ' + h.outcome}>{h.outcome}</span> {h.message}
                  </div>
                ))}
              </div>
              <div className="panel">
                <h2>Main factors</h2>
                <ReasonBars reasons={result.reasons} />
              </div>
            </>
          )}
        </div>
      </div>
    </>
  )
}
