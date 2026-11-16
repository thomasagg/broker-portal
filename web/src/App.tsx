import { Navigate, NavLink, Outlet, Route, Routes } from 'react-router'
import { useAuth } from './auth'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import SubmissionsPage from './pages/SubmissionsPage'
import SubmissionFormPage from './pages/SubmissionFormPage'
import SubmissionPage from './pages/SubmissionPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<LoggedIn />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/submissions" element={<SubmissionsPage />} />
        <Route path="/submissions/new" element={<SubmissionFormPage />} />
        <Route path="/submissions/:id" element={<SubmissionPage />} />
        <Route path="/submissions/:id/edit" element={<SubmissionFormPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function LoggedIn() {
  const { user, loading, logout } = useAuth()
  if (loading) return <div className="page-loading">Loading…</div>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <img src="/favicon.svg" alt="" width={26} height={26} />
            <span>Broker Portal</span>
          </NavLink>
          <nav className="nav">
            <NavLink to="/" end>
              Dashboard
            </NavLink>
            <NavLink to="/submissions" end>
              Submissions
            </NavLink>
            {user.role === 'BROKER' && <NavLink to="/submissions/new">New submission</NavLink>}
          </nav>
          <div className="who">
            <span className="who-name">{user.fullName}</span>
            <span className="who-org">{user.brokerageName ?? 'Underwriting'}</span>
            <button className="link-button" onClick={logout}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
