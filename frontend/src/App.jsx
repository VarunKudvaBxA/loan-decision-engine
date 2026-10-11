import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { clearSession, getUser } from './api.js'
import Login from './pages/Login.jsx'
import Applications from './pages/Applications.jsx'
import NewApplication from './pages/NewApplication.jsx'
import ApplicationDetail from './pages/ApplicationDetail.jsx'
import Simulator from './pages/Simulator.jsx'
import Rules from './pages/Rules.jsx'

function Shell({ children }) {
  const user = getUser()
  const navigate = useNavigate()
  if (!user) return <Navigate to="/login" replace />

  const logout = () => {
    clearSession()
    navigate('/login')
  }

  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">Loan desk</div>
        <nav>
          <NavLink to="/" end>Applications</NavLink>
          <NavLink to="/new">New application</NavLink>
          <NavLink to="/simulator">What-if simulator</NavLink>
          <NavLink to="/rules">Policy rules</NavLink>
        </nav>
        <div className="who">
          <div>{user.username}</div>
          <div className="muted">{user.role.toLowerCase()}</div>
          <button className="link" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Shell><Applications /></Shell>} />
      <Route path="/new" element={<Shell><NewApplication /></Shell>} />
      <Route path="/applications/:id" element={<Shell><ApplicationDetail /></Shell>} />
      <Route path="/simulator" element={<Shell><Simulator /></Shell>} />
      <Route path="/rules" element={<Shell><Rules /></Shell>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
