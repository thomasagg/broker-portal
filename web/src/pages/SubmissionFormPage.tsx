import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { api, ApiError } from '../api'
import { FIELD_LABEL, LINE_LABEL } from '../labels'
import type { LineOfBusiness, SubmissionDetails } from '../types'

const EMPTY: SubmissionDetails = {
  lineOfBusiness: 'PROPERTY',
  insuredName: '',
  insuredCountry: 'GR',
  insuredIndustry: '',
  annualRevenue: null,
  sumInsured: null,
  currency: 'EUR',
  coverageStart: null,
  coverageEnd: null,
  targetPremium: null,
  riskDescription: '',
}

// empty inputs go to the API as null, not "", so optional fields stay optional
function clean(d: SubmissionDetails): SubmissionDetails {
  const blank = (v: string | null) => (v && v.trim() ? v.trim() : null)
  return {
    ...d,
    insuredCountry: blank(d.insuredCountry),
    insuredIndustry: blank(d.insuredIndustry),
    riskDescription: blank(d.riskDescription),
    coverageStart: d.coverageStart || null,
    coverageEnd: d.coverageEnd || null,
  }
}

export default function SubmissionFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState<SubmissionDetails>(EMPTY)
  const [status, setStatus] = useState<string | null>(null)
  const [missing, setMissing] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [loaded, setLoaded] = useState(!id)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    api.submission(id).then(({ submission: s }) => {
      setForm({
        lineOfBusiness: s.lineOfBusiness,
        insuredName: s.insuredName,
        insuredCountry: s.insuredCountry,
        insuredIndustry: s.insuredIndustry,
        annualRevenue: s.annualRevenue,
        sumInsured: s.sumInsured,
        currency: s.currency,
        coverageStart: s.coverageStart,
        coverageEnd: s.coverageEnd,
        targetPremium: s.targetPremium,
        riskDescription: s.riskDescription,
      })
      setStatus(s.status)
      setMissing(s.missingFields)
      setLoaded(true)
    }, (e) => setLoadError(e instanceof Error ? e.message : 'Could not load the submission'))
  }, [id])

  function set<K extends keyof SubmissionDetails>(key: K, value: SubmissionDetails[K]) {
    setForm((f) => ({ ...f, [key]: value }))
    setMissing((m) => m.filter((f) => f !== key))
  }

  const num = (v: string) => (v === '' ? null : Number(v))

  async function save(andSubmit: boolean) {
    setBusy(true)
    setError(null)
    try {
      const saved = id ? await api.update(id, clean(form)) : await api.create(clean(form))
      const savedId = saved.submission.id
      if (andSubmit) {
        try {
          await api.act(savedId, { action: 'SUBMIT' })
        } catch (err) {
          // the draft is saved, stay on the form and show what's missing
          if (err instanceof ApiError && err.missingFields.length > 0) {
            setMissing(err.missingFields)
            setError('Saved as draft. Fill in the highlighted fields to submit.')
            if (!id) navigate(`/submissions/${savedId}/edit`, { replace: true, state: { savedAsDraft: true } })
            return
          }
          throw err
        }
      }
      navigate(`/submissions/${savedId}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save')
    } finally {
      setBusy(false)
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    save(status !== 'INFO_REQUESTED')
  }

  if (loadError) return <p className="error">{loadError}</p>
  if (!loaded) return <p className="muted">Loading…</p>

  const field = (key: keyof SubmissionDetails) => ({
    className: missing.includes(key) ? 'field field-missing' : 'field',
  })

  return (
    <form onSubmit={onSubmit} className="form-page">
      <div className="page-head">
        <div>
          <h1>{id ? 'Edit submission' : 'New submission'}</h1>
          <p className="muted">
            {status === 'INFO_REQUESTED'
              ? 'Update what the underwriter asked for, then send the info back from the submission page.'
              : 'Fields marked * are needed before underwriting can start. You can save a draft any time.'}
          </p>
        </div>
      </div>

      <div className="grid-form">
        <div className="card">
          <h2>Insured</h2>
          <label {...field('insuredName')}>
            {FIELD_LABEL.insuredName} *
            <input value={form.insuredName} onChange={(e) => set('insuredName', e.target.value)} required maxLength={200} />
          </label>
          <div className="row">
            <label {...field('insuredCountry')}>
              {FIELD_LABEL.insuredCountry} *
              <input
                placeholder="2-letter code, e.g. GR"
                value={form.insuredCountry ?? ''}
                onChange={(e) => set('insuredCountry', e.target.value.toUpperCase())}
                maxLength={2}
              />
            </label>
            <label {...field('insuredIndustry')}>
              {FIELD_LABEL.insuredIndustry} *
              <input value={form.insuredIndustry ?? ''} onChange={(e) => set('insuredIndustry', e.target.value)} />
            </label>
          </div>
          <label {...field('annualRevenue')}>
            {FIELD_LABEL.annualRevenue}
            <input
              type="number"
              min={1}
              value={form.annualRevenue ?? ''}
              onChange={(e) => set('annualRevenue', num(e.target.value))}
            />
          </label>
        </div>

        <div className="card">
          <h2>Cover</h2>
          <label {...field('lineOfBusiness')}>
            {FIELD_LABEL.lineOfBusiness} *
            <select
              value={form.lineOfBusiness}
              onChange={(e) => set('lineOfBusiness', e.target.value as LineOfBusiness)}
            >
              {Object.entries(LINE_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <div className="row">
            <label {...field('sumInsured')}>
              {FIELD_LABEL.sumInsured} *
              <input
                type="number"
                min={1}
                value={form.sumInsured ?? ''}
                onChange={(e) => set('sumInsured', num(e.target.value))}
              />
            </label>
            <label {...field('currency')}>
              {FIELD_LABEL.currency}
              <select value={form.currency ?? 'EUR'} onChange={(e) => set('currency', e.target.value)}>
                <option>EUR</option>
                <option>USD</option>
                <option>GBP</option>
              </select>
            </label>
          </div>
          <div className="row">
            <label {...field('coverageStart')}>
              {FIELD_LABEL.coverageStart} *
              <input
                type="date"
                value={form.coverageStart ?? ''}
                onChange={(e) => set('coverageStart', e.target.value)}
              />
            </label>
            <label {...field('coverageEnd')}>
              {FIELD_LABEL.coverageEnd} *
              <input type="date" value={form.coverageEnd ?? ''} onChange={(e) => set('coverageEnd', e.target.value)} />
            </label>
          </div>
          <label {...field('targetPremium')}>
            {FIELD_LABEL.targetPremium}
            <input
              type="number"
              min={1}
              value={form.targetPremium ?? ''}
              onChange={(e) => set('targetPremium', num(e.target.value))}
            />
          </label>
        </div>

        <div className="card span-2">
          <h2>Risk</h2>
          <label {...field('riskDescription')}>
            {FIELD_LABEL.riskDescription} *
            <textarea
              rows={6}
              maxLength={5000}
              placeholder="Locations, construction, protections, claims history, anything the underwriter will ask about."
              value={form.riskDescription ?? ''}
              onChange={(e) => set('riskDescription', e.target.value)}
            />
          </label>
        </div>
      </div>

      {(error || location.state?.savedAsDraft) && missing.length > 0 && (
        <p className="hint">{error ?? 'Saved as draft. Fill in the highlighted fields to submit.'}</p>
      )}
      {missing.length > 0 && (
        <p className="error">Still missing: {missing.map((m) => FIELD_LABEL[m] ?? m).join(', ')}</p>
      )}
      {error && missing.length === 0 && <p className="error">{error}</p>}

      <div className="form-actions">
        <Link to={id ? `/submissions/${id}` : '/'} className="button">
          Cancel
        </Link>
        {status !== 'INFO_REQUESTED' && (
          <button type="button" className="button" disabled={busy} onClick={() => save(false)}>
            Save draft
          </button>
        )}
        <button className="button primary" disabled={busy}>
          {status === 'INFO_REQUESTED' ? 'Save changes' : 'Save and submit'}
        </button>
      </div>
    </form>
  )
}
