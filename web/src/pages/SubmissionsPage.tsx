import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { api } from '../api'
import { useUser } from '../auth'
import { SubmissionTable } from '../components'
import { STATUS_LABEL } from '../labels'
import type { Status, Submission } from '../types'

const GROUPS: { label: string; statuses: Status[] }[] = [
  { label: 'All', statuses: [] },
  { label: 'Drafts', statuses: ['DRAFT'] },
  { label: 'With underwriting', statuses: ['SUBMITTED', 'IN_REVIEW', 'BIND_REQUESTED'] },
  { label: 'Info requested', statuses: ['INFO_REQUESTED'] },
  { label: 'Quoted', statuses: ['QUOTED'] },
  { label: 'Bound & booked', statuses: ['BOUND', 'BOOKED'] },
  { label: 'Closed', statuses: ['DECLINED', 'NOT_TAKEN_UP', 'WITHDRAWN', 'CANCELLED'] },
]

export default function SubmissionsPage() {
  const user = useUser()
  const [params, setParams] = useSearchParams()
  const statuses = params.getAll('status') as Status[]
  const [query, setQuery] = useState(params.get('q') ?? '')
  const [rows, setRows] = useState<Submission[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const statusKey = statuses.join(',')
  const q = params.get('q') ?? ''

  useEffect(() => {
    setRows(null)
    api
      .submissions(statusKey ? (statusKey.split(',') as Status[]) : [], q)
      .then(setRows)
      .catch((e) => setError(e.message))
  }, [statusKey, q])

  function pick(next: Status[]) {
    const p = new URLSearchParams()
    next.forEach((s) => p.append('status', s))
    if (q) p.set('q', q)
    setParams(p)
  }

  function search(value: string) {
    setQuery(value)
    const p = new URLSearchParams(params)
    if (value.trim()) p.set('q', value.trim())
    else p.delete('q')
    setParams(p, { replace: true })
  }

  const groups = user.role === 'BROKER' ? GROUPS : GROUPS.filter((g) => g.label !== 'Drafts')
  const custom = statuses.length > 0 && !groups.some((g) => g.statuses.join(',') === statusKey)

  return (
    <>
      <div className="page-head">
        <h1>Submissions</h1>
        <input
          className="search"
          type="search"
          placeholder="Search insured or reference"
          value={query}
          onChange={(e) => search(e.target.value)}
        />
      </div>
      <div className="chips">
        {groups.map((g) => (
          <button
            key={g.label}
            className={g.statuses.join(',') === statusKey ? 'chip chip-on' : 'chip'}
            onClick={() => pick(g.statuses)}
          >
            {g.label}
          </button>
        ))}
        {custom && <span className="chip chip-on">{statuses.map((s) => STATUS_LABEL[s]).join(', ')}</span>}
      </div>
      {error && <p className="error">{error}</p>}
      <div className="card">
        {rows ? <SubmissionTable rows={rows} empty="No submissions match." /> : <p className="muted">Loading…</p>}
      </div>
    </>
  )
}
