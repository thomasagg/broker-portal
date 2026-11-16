import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { useAuth } from '../auth'

const DEMO_ACCOUNTS = [
  { email: 'maria@aegean.example', who: 'Broker, Aegean Insurance Brokers' },
  { email: 'eleni@northern.example', who: 'Broker, Northern Risk Partners' },
  { email: 'anna@insurer.example', who: 'Underwriter' },
]

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <form className="card login-card" onSubmit={submit}>
        <div className="brand brand-large">
          <img src="/favicon.svg" alt="" width={36} height={36} />
          <span>Broker Portal</span>
        </div>
        <p className="muted">Submit risks, follow every quote and see your policies in one place.</p>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="button primary" disabled={busy}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <div className="demo-accounts">
          <p className="small muted">Demo accounts (password demo1234):</p>
          {DEMO_ACCOUNTS.map((a) => (
            <button
              type="button"
              key={a.email}
              className="demo-account"
              onClick={() => {
                setEmail(a.email)
                setPassword('demo1234')
              }}
            >
              <strong>{a.email}</strong>
              <span>{a.who}</span>
            </button>
          ))}
        </div>
      </form>
    </div>
  )
}
