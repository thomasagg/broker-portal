import { Link } from 'react-router'
import { LIFECYCLE, LINE_LABEL, money, STATUS_LABEL, STATUS_TONE } from './labels'
import type { Status, Submission } from './types'

export function StatusPill({ status }: { status: Status }) {
  return <span className={`pill pill-${STATUS_TONE[status]}`}>{STATUS_LABEL[status]}</span>
}

// INFO_REQUESTED sits between submitted and quoted, so it lights up the review step
export function Stepper({ status }: { status: Status }) {
  const shown: Status = status === 'INFO_REQUESTED' ? 'IN_REVIEW' : status
  const current = LIFECYCLE.indexOf(shown)
  const closed = STATUS_TONE[status] === 'closed'

  return (
    <ol className="stepper">
      {LIFECYCLE.map((step, i) => {
        const state = current > i || status === 'BOOKED' ? 'done' : current === i ? 'current' : 'todo'
        return (
          <li key={step} className={`step step-${closed ? 'todo' : state}`}>
            <span className="step-dot" />
            <span className="step-label">
              {step === 'IN_REVIEW' && status === 'INFO_REQUESTED' ? 'Info requested' : STATUS_LABEL[step]}
            </span>
          </li>
        )
      })}
      {closed && (
        <li className="step step-closed">
          <span className="step-dot" />
          <span className="step-label">{STATUS_LABEL[status]}</span>
        </li>
      )}
    </ol>
  )
}

export function SubmissionTable({ rows, empty }: { rows: Submission[]; empty: string }) {
  if (rows.length === 0) return <p className="empty">{empty}</p>

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Reference</th>
            <th>Insured</th>
            <th>Line</th>
            <th>Status</th>
            <th className="num">In status</th>
            <th className="num">Premium</th>
            <th>Broker</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((s) => (
            <tr key={s.id}>
              <td>
                <Link to={`/submissions/${s.id}`} className="ref">
                  {s.reference}
                </Link>
              </td>
              <td>{s.insuredName}</td>
              <td>{LINE_LABEL[s.lineOfBusiness]}</td>
              <td>
                <StatusPill status={s.status} />
              </td>
              <td className={`num ${s.daysInStatus >= 3 && STATUS_TONE[s.status] !== 'closed' ? 'stale' : ''}`}>
                {s.daysInStatus === 0 ? 'today' : `${s.daysInStatus} d`}
              </td>
              <td className="num">{money(s.quotedPremium ?? s.targetPremium, s.currency)}</td>
              <td>
                <div>{s.brokerName}</div>
                <div className="muted small">{s.brokerageName}</div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
