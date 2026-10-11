import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, money, pct } from '../api.js'

export default function Applications() {
  const [rows, setRows] = useState(null)
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([api('/applications'), api('/stats')])
      .then(([r, s]) => {
        setRows(r)
        setStats(s)
      })
      .catch((e) => setError(e.message))
  }, [])

  if (error) return <div className="error">{error}</div>
  if (!rows) return <p className="muted">Loading applications...</p>

  const pending = rows.filter((r) => r.status.startsWith('PENDING')).length

  return (
    <>
      <div className="page-head">
        <h1>Applications</h1>
        <p className="muted">
          {rows.length} total, {pending} waiting for a person.
          {stats && stats.total > 0 && ' Average default probability ' + pct(stats.averageRisk) + '.'}
        </p>
      </div>

      {stats && stats.total > 0 && (
        <div className="panel">
          <h2>How risky the book looks</h2>
          <div style={{ height: 180 }}>
            <ResponsiveContainer>
              <BarChart data={stats.riskHistogram}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="range" fontSize={12} />
                <YAxis allowDecimals={false} fontSize={12} width={30} />
                <Tooltip />
                <Bar dataKey="count" name="Applications" fill="#14675a" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      <div className="panel">
        {rows.length === 0 ? (
          <p>
            No applications yet. <Link to="/new">Create the first one</Link>.
          </p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Applicant</th>
                <th className="num">Amount</th>
                <th className="num">Default risk</th>
                <th>System says</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="click" onClick={() => navigate('/applications/' + r.id)}>
                  <td>{r.id}</td>
                  <td>{r.applicantName}</td>
                  <td className="num">{money(r.loanAmount)}</td>
                  <td className="num">{r.riskScore == null ? '-' : pct(r.riskScore)}</td>
                  <td><span className={'tag ' + r.systemRecommendation}>{r.systemRecommendation}</span></td>
                  <td><span className={'tag ' + r.status}>{r.status.replace('_', ' ').toLowerCase()}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
