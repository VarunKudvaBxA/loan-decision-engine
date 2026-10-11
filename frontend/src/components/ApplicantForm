const fields = [
  { key: 'age', label: 'Age', step: 1 },
  { key: 'annualIncome', label: 'Annual income (USD)', step: 1000 },
  { key: 'monthlyDebt', label: 'Monthly debt payments (USD)', step: 50 },
  { key: 'loanAmount', label: 'Loan amount (USD)', step: 500 },
  { key: 'termMonths', label: 'Term (months)', step: 6 },
  { key: 'creditScore', label: 'Credit score', step: 5 },
  { key: 'delinquencies', label: 'Delinquencies in last 2 years', step: 1 },
  { key: 'employmentYears', label: 'Years at current employer', step: 0.5 },
]

export const emptyApplicant = {
  applicantName: '',
  age: 34,
  annualIncome: 72000,
  monthlyDebt: 600,
  loanAmount: 18000,
  termMonths: 48,
  creditScore: 690,
  delinquencies: 0,
  employmentYears: 4,
}

export function toPayload(form) {
  const out = { applicantName: form.applicantName }
  fields.forEach((f) => {
    out[f.key] = Number(form[f.key])
  })
  return out
}

export default function ApplicantForm({ form, onChange, showName = true }) {
  const set = (key) => (e) => onChange({ ...form, [key]: e.target.value })
  return (
    <div className="form-grid">
      {showName && (
        <div className="wide">
          <label htmlFor="name">Applicant name</label>
          <input id="name" value={form.applicantName} onChange={set('applicantName')} />
        </div>
      )}
      {fields.map((f) => (
        <div key={f.key}>
          <label htmlFor={f.key}>{f.label}</label>
          <input id={f.key} type="number" step={f.step} value={form[f.key]} onChange={set(f.key)} />
        </div>
      ))}
    </div>
  )
}
