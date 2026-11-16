import type { Action, LineOfBusiness, Status } from './types'

export const STATUS_LABEL: Record<Status, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  IN_REVIEW: 'In review',
  INFO_REQUESTED: 'Info requested',
  QUOTED: 'Quoted',
  BIND_REQUESTED: 'Bind requested',
  BOUND: 'Bound',
  BOOKED: 'Booked',
  DECLINED: 'Declined',
  NOT_TAKEN_UP: 'Not taken up',
  WITHDRAWN: 'Withdrawn',
  CANCELLED: 'Cancelled',
}

// the tone drives the pill colour: who has the ball, or how it ended
export const STATUS_TONE: Record<Status, 'neutral' | 'waiting' | 'action' | 'good' | 'closed'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'waiting',
  IN_REVIEW: 'waiting',
  INFO_REQUESTED: 'action',
  QUOTED: 'action',
  BIND_REQUESTED: 'waiting',
  BOUND: 'good',
  BOOKED: 'good',
  DECLINED: 'closed',
  NOT_TAKEN_UP: 'closed',
  WITHDRAWN: 'closed',
  CANCELLED: 'closed',
}

export const LIFECYCLE: Status[] = ['SUBMITTED', 'IN_REVIEW', 'QUOTED', 'BIND_REQUESTED', 'BOUND', 'BOOKED']

export const ACTION_LABEL: Record<Action, string> = {
  SUBMIT: 'Submit to underwriting',
  START_REVIEW: 'Start review',
  REQUEST_INFO: 'Request info',
  PROVIDE_INFO: 'Send info back',
  QUOTE: 'Issue quote',
  DECLINE: 'Decline',
  REQUEST_BIND: 'Request to bind',
  REJECT_QUOTE: 'Reject quote',
  BIND: 'Bind',
  BOOK: 'Book policy',
  WITHDRAW: 'Withdraw',
  CANCEL: 'Cancel policy',
}

// same list as needsNote on the backend
export const NEEDS_NOTE: Action[] = ['REQUEST_INFO', 'PROVIDE_INFO', 'DECLINE', 'REJECT_QUOTE', 'WITHDRAW', 'CANCEL']

export const DESTRUCTIVE: Action[] = ['DECLINE', 'REJECT_QUOTE', 'WITHDRAW', 'CANCEL']

export const LINE_LABEL: Record<LineOfBusiness, string> = {
  PROPERTY: 'Property',
  GENERAL_LIABILITY: 'General liability',
  PROFESSIONAL_INDEMNITY: 'Professional indemnity',
  CYBER: 'Cyber',
  MARINE_CARGO: 'Marine cargo',
  DIRECTORS_AND_OFFICERS: 'Directors & officers',
}

export const FIELD_LABEL: Record<string, string> = {
  lineOfBusiness: 'Line of business',
  insuredName: 'Insured name',
  insuredCountry: 'Country',
  insuredIndustry: 'Industry',
  annualRevenue: 'Annual revenue',
  sumInsured: 'Sum insured / limit',
  currency: 'Currency',
  coverageStart: 'Coverage start',
  coverageEnd: 'Coverage end',
  targetPremium: 'Target premium',
  riskDescription: 'Risk description',
}

export function money(amount: number | null, currency: string | null = 'EUR') {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: currency ?? 'EUR',
    maximumFractionDigits: 0,
  }).format(amount)
}

export function date(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function dateTime(value: string) {
  return new Date(value).toLocaleString('en-GB', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function duration(hours: number | null) {
  if (hours == null) return '—'
  if (hours < 24) return `${Math.round(hours)} h`
  return `${(hours / 24).toFixed(1)} days`
}

export function daysUntil(value: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((new Date(value).getTime() - today.getTime()) / 86_400_000)
}
