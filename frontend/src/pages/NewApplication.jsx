import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import ApplicantForm, { emptyApplicant, toPayload } from '../components/ApplicantForm.jsx'

export default function NewApplication() {
  const [form, setForm] = useState(emptyApplicant)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const res = await api('/applications', { method: 'POST', body: toPayload(form) })
      navigate('/applications/' + res.id)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>New application</h1>
        <p className="muted">The system scores it straight away. A person still has to review and approve.</p>
      </div>
      <form className="panel" onSubmit={submit}>
        <ApplicantForm form={form} onChange={setForm} />
        {error && <div className="error">{error}</div>}
        <div style={{ marginTop: 20 }}>
          <button type="submit" disabled={busy}>{busy ? 'Scoring...' : 'Score application'}</button>
        </div>
      </form>
    </>
  )
}
