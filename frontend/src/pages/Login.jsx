import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, saveSession } from '../api.js'

export default function Login() {
  const [username, setUsername] = useState('analyst')
  const [password, setPassword] = useState('analyst123')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const data = await api('/auth/login', { method: 'POST', body: { username, password } })
      saveSession(data)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="login-wrap">
      <form className="panel login-box" onSubmit={submit}>
        <h1>Loan desk</h1>
        <p className="muted">Sign in to review applications.</p>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="u">Username</label>
          <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="p">Password</label>
          <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        {error && <div className="error">{error}</div>}
        <button type="submit">Sign in</button>
        <p className="muted small" style={{ marginTop: 16 }}>
          Demo users: analyst / analyst123, senior / senior123
        </p>
      </form>
    </div>
  )
}
