import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api'
import { useUser } from '../auth'
import { StatusPill, Stepper } from '../components'
import {
  ACTION_LABEL,
  date,
  dateTime,
  daysUntil,
  DESTRUCTIVE,
  FIELD_LABEL,
  LINE_LABEL,
  money,
  NEEDS_NOTE,
  STATUS_LABEL,
} from '../labels'
import type { Action, SubmissionDetail } from '../types'

export default function SubmissionPage() {
  const { id } = useParams() as { id: string }
  const user = useUser()
  const navigate = useNavigate()
  const [data, setData] = useState<SubmissionDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState<Action | null>(null)

  useEffect(() => {
    setData(null)
    api
      .submission(id)
      .then(setData)
      .catch((e) => setError(e.message))
  }, [id])

  if (error && !data) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Loading…</p>

  const s = data.submission
  const isBroker = user.role === 'BROKER'
  const canEdit = isBroker && (s.status === 'DRAFT' || s.status === 'INFO_REQUESTED')

  async function run(action: Action, extra: Record<string, unknown> = {}) {
    setError(null)
    try {
      setData(await api.act(id, { action, ...extra }))
      setPending(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong')
    }
  }

  function click(action: Action) {
    if (action === 'SUBMIT' && s.missingFields.length > 0) {
      navigate(`/submissions/${id}/edit`)
      return
    }
    if (action === 'QUOTE' || NEEDS_NOTE.includes(action)) setPending(action)
    else run(action)
  }

  async function copy() {
    const created = await api.copy(id)
    navigate(`/submissions/${created.submission.id}/edit`)
  }

  // status changes and messages read as one conversation
  const timeline = [
    ...data.events.map((e) => ({ kind: 'event' as const, at: e.createdAt, event: e })),
    ...data.messages.map((m) => ({ kind: 'message' as const, at: m.createdAt, message: m })),
  ].sort((a, b) => a.at.localeCompare(b.at))

  return (
    <>
      <div className="page-head">
        <div>
          <p className="muted small">
            {s.reference} · {LINE_LABEL[s.lineOfBusiness]}
          </p>
          <h1>
            {s.insuredName} <StatusPill status={s.status} />
          </h1>
          <p className="muted small">
            {STATUS_LABEL[s.status]} {s.daysInStatus === 0 ? 'since today' : `for ${s.daysInStatus} days`} ·{' '}
            {s.brokerName}, {s.brokerageName}
            {s.underwriterName && ` · Underwriter: ${s.underwriterName}`}
          </p>
        </div>
        <div className="head-actions">
          {canEdit && (
            <Link to={`/submissions/${id}/edit`} className="button">
              Edit
            </Link>
          )}
          {isBroker && (
            <button className="button" onClick={copy} title="Start a new draft from this one, dates moved a year on">
              Copy as renewal
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <Stepper status={s.status} />
      </div>

      {s.availableActions.length > 0 && (
        <div className="card actions-card">
          <div className="actions">
            {s.availableActions.map((a) => (
              <button
                key={a}
                className={`button ${DESTRUCTIVE.includes(a) ? 'danger' : a === pending ? 'primary' : 'primary-soft'}`}
                onClick={() => click(a)}
              >
                {ACTION_LABEL[a]}
              </button>
            ))}
          </div>
          {s.status === 'DRAFT' && s.missingFields.length > 0 && (
            <p className="hint">
              Before submitting, add: {s.missingFields.map((f) => FIELD_LABEL[f] ?? f).join(', ')}
            </p>
          )}
          {pending && <ActionForm action={pending} onCancel={() => setPending(null)} onSend={(x) => run(pending, x)} />}
          {error && <p className="error">{error}</p>}
        </div>
      )}

      <div className="grid-2">
        <div className="stack">
          {s.quotedPremium != null && (
            <div className={`card quote ${s.status === 'QUOTED' ? 'quote-live' : ''}`}>
              <h2>{s.policyNumber ? `Policy ${s.policyNumber}` : 'Quote'}</h2>
              <div className="quote-premium">{money(s.quotedPremium, s.currency)}</div>
              <p className="muted small">
                Valid until {date(s.quoteValidUntil)}
                {s.status === 'QUOTED' && ` (${Math.max(daysUntil(s.quoteValidUntil!), 0)} days left)`}
              </p>
              {s.quoteTerms && <p>{s.quoteTerms}</p>}
            </div>
          )}
          {s.closedReason && (
            <div className="card closed">
              <h2>Why it closed</h2>
              <p>{s.closedReason}</p>
            </div>
          )}
          <div className="card">
            <h2>Risk details</h2>
            <dl className="details">
              <dt>Country</dt>
              <dd>{s.insuredCountry ?? '—'}</dd>
              <dt>Industry</dt>
              <dd>{s.insuredIndustry ?? '—'}</dd>
              <dt>Annual revenue</dt>
              <dd>{money(s.annualRevenue, s.currency)}</dd>
              <dt>Sum insured</dt>
              <dd>{money(s.sumInsured, s.currency)}</dd>
              <dt>Coverage</dt>
              <dd>
                {date(s.coverageStart)} – {date(s.coverageEnd)}
              </dd>
              <dt>Target premium</dt>
              <dd>{money(s.targetPremium, s.currency)}</dd>
            </dl>
            <p className="risk">{s.riskDescription ?? <span className="muted">No description yet.</span>}</p>
          </div>
        </div>

        <div className="card">
          <h2>Timeline</h2>
          <ol className="timeline">
            {timeline.map((item) =>
              item.kind === 'event' ? (
                <li key={item.event.id} className="tl-event">
                  <span className="tl-dot" />
                  <div>
                    <div>
                      <StatusPill status={item.event.toStatus} />{' '}
                      <span className="muted small">
                        {item.event.actorName ?? 'System'} · {dateTime(item.at)}
                      </span>
                    </div>
                    {item.event.note && <p className="tl-note">{item.event.note}</p>}
                  </div>
                </li>
              ) : (
                <li key={item.message.id} className={`tl-message tl-${item.message.authorRole.toLowerCase()}`}>
                  <div className="bubble">
                    <div className="muted small">
                      {item.message.authorName} · {dateTime(item.at)}
                    </div>
                    <p>{item.message.body}</p>
                  </div>
                </li>
              ),
            )}
          </ol>
          {s.status !== 'DRAFT' && <MessageBox onSend={async (body) => setData(await api.message(id, body))} />}
        </div>
      </div>
    </>
  )
}

function ActionForm({
  action,
  onCancel,
  onSend,
}: {
  action: Action
  onCancel: () => void
  onSend: (extra: Record<string, unknown>) => void
}) {
  const [note, setNote] = useState('')
  const [premium, setPremium] = useState('')
  const [validUntil, setValidUntil] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() + 30)
    return d.toISOString().slice(0, 10)
  })
  const [terms, setTerms] = useState('')

  function submit(e: FormEvent) {
    e.preventDefault()
    if (action === 'QUOTE') onSend({ premium: Number(premium), validUntil, terms, note })
    else onSend({ note })
  }

  return (
    <form className="action-form" onSubmit={submit}>
      <h3>{ACTION_LABEL[action]}</h3>
      {action === 'QUOTE' && (
        <div className="row">
          <label className="field">
            Premium
            <input type="number" min={1} step="0.01" value={premium} onChange={(e) => setPremium(e.target.value)} required />
          </label>
          <label className="field">
            Valid until
            <input type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)} required />
          </label>
        </div>
      )}
      {action === 'QUOTE' && (
        <label className="field">
          Terms and subjectivities
          <textarea rows={3} value={terms} onChange={(e) => setTerms(e.target.value)} />
        </label>
      )}
      <label className="field">
        {action === 'REQUEST_INFO'
          ? 'What do you need from the broker?'
          : action === 'PROVIDE_INFO'
            ? 'What did you add or change?'
            : action === 'QUOTE'
              ? 'Note (optional)'
              : 'Reason'}
        <textarea rows={3} value={note} onChange={(e) => setNote(e.target.value)} required={NEEDS_NOTE.includes(action)} />
      </label>
      <div className="form-actions">
        <button type="button" className="button" onClick={onCancel}>
          Back
        </button>
        <button className={`button ${DESTRUCTIVE.includes(action) ? 'danger' : 'primary'}`}>{ACTION_LABEL[action]}</button>
      </div>
    </form>
  )
}

function MessageBox({ onSend }: { onSend: (body: string) => Promise<void> }) {
  const [body, setBody] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!body.trim()) return
    setBusy(true)
    try {
      await onSend(body)
      setBody('')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="message-box" onSubmit={submit}>
      <textarea
        rows={2}
        placeholder="Write a message to the other side…"
        value={body}
        onChange={(e) => setBody(e.target.value)}
        maxLength={4000}
      />
      <button className="button primary" disabled={busy || !body.trim()}>
        Send
      </button>
    </form>
  )
}
