import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api'
import { useUser } from '../auth'
import { StatusPill, SubmissionTable } from '../components'
import { daysUntil, duration, money, STATUS_LABEL } from '../labels'
import type { Dashboard, Status } from '../types'

const plural = (n: number) => `${n} submission${n === 1 ? '' : 's'}`

const PIPELINE: Status[] = ['DRAFT', 'SUBMITTED', 'IN_REVIEW', 'INFO_REQUESTED', 'QUOTED', 'BIND_REQUESTED', 'BOUND', 'BOOKED']

export default function DashboardPage() {
  const user = useUser()
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.dashboard().then(setData).catch((e) => setError(e.message))
  }, [])

  if (error) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Loading…</p>

  const stages = user.role === 'BROKER' ? PIPELINE : PIPELINE.filter((s) => s !== 'DRAFT')
  const t = data.turnaround

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Good to see you, {user.fullName.split(' ')[0]}</h1>
          <p className="muted">{user.role === 'BROKER' ? user.brokerageName : 'Underwriting desk'}</p>
        </div>
        {user.role === 'BROKER' && (
          <Link to="/submissions/new" className="button primary">
            New submission
          </Link>
        )}
      </div>

      <section className="pipeline">
        {stages.map((s) => (
          <Link key={s} to={`/submissions?status=${s}`} className="stage">
            <span className="stage-count">{data.countsByStatus[s] ?? 0}</span>
            <span className="stage-label">{STATUS_LABEL[s]}</span>
          </Link>
        ))}
      </section>

      <section className="grid-2">
        <div className="card">
          <h2>Waiting on you</h2>
          <p className="muted small">Oldest first. These need your move before anything else can happen.</p>
          <SubmissionTable rows={data.waitingOnMe} empty="Nothing waiting on you right now." compact />
        </div>

        <div className="stack">
          <div className="card">
            <h2>Quotes expiring soon</h2>
            {data.expiringQuotes.length === 0 ? (
              <p className="empty">No quotes expire in the next 7 days.</p>
            ) : (
              <ul className="expiring">
                {data.expiringQuotes.map((s) => {
                  const days = daysUntil(s.quoteValidUntil!)
                  return (
                    <li key={s.id}>
                      <Link to={`/submissions/${s.id}`}>
                        <strong>{s.insuredName}</strong>
                        <span className="muted small">
                          {s.reference} · {money(s.quotedPremium, s.currency)}
                        </span>
                      </Link>
                      <span className={days <= 2 ? 'badge badge-urgent' : 'badge'}>
                        {days <= 0 ? 'today' : `${days} d left`}
                      </span>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          <div className="card">
            <h2>Turnaround, last 90 days</h2>
            <div className="kpis">
              <div className="kpi">
                <span className="kpi-value">{duration(t.submittedToQuotedHours)}</span>
                <span className="kpi-label">
                  <StatusPill status="SUBMITTED" /> to <StatusPill status="QUOTED" />
                </span>
                <span className="muted small">{plural(t.quotedSample)}</span>
              </div>
              <div className="kpi">
                <span className="kpi-value">{duration(t.submittedToBoundHours)}</span>
                <span className="kpi-label">
                  <StatusPill status="SUBMITTED" /> to <StatusPill status="BOUND" />
                </span>
                <span className="muted small">{plural(t.boundSample)}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
