import type { ActionRequest, Dashboard, Status, SubmissionDetail, SubmissionDetails, Submission, User } from './types'

const TOKEN_KEY = 'broker-portal-token'

export class ApiError extends Error {
  status: number
  missingFields: string[]

  constructor(status: number, message: string, missingFields: string[] = []) {
    super(message)
    this.status = status
    this.missingFields = missingFields
  }
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // private mode etc., the user just logs in again next time
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })

  if (res.status === 401 && path !== '/api/auth/login') {
    setToken(null)
    window.location.assign('/login')
  }
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    throw new ApiError(res.status, problem.detail ?? `Request failed (${res.status})`, problem.missingFields ?? [])
  }
  return res.json() as Promise<T>
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string; user: User }>('POST', '/api/auth/login', { email, password }),
  me: () => request<User>('GET', '/api/auth/me'),
  dashboard: () => request<Dashboard>('GET', '/api/dashboard'),
  submissions: (statuses: Status[], q: string) => {
    const params = new URLSearchParams()
    statuses.forEach((s) => params.append('status', s))
    if (q.trim()) params.set('q', q.trim())
    return request<Submission[]>('GET', `/api/submissions?${params}`)
  },
  submission: (id: string) => request<SubmissionDetail>('GET', `/api/submissions/${id}`),
  create: (details: SubmissionDetails) => request<SubmissionDetail>('POST', '/api/submissions', details),
  update: (id: string, details: SubmissionDetails) =>
    request<SubmissionDetail>('PUT', `/api/submissions/${id}`, details),
  copy: (id: string) => request<SubmissionDetail>('POST', `/api/submissions/${id}/copy`),
  act: (id: string, action: ActionRequest) =>
    request<SubmissionDetail>('POST', `/api/submissions/${id}/actions`, action),
  message: (id: string, body: string) =>
    request<SubmissionDetail>('POST', `/api/submissions/${id}/messages`, { body }),
}
