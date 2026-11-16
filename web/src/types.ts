export type Role = 'BROKER' | 'UNDERWRITER'

export type Status =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'INFO_REQUESTED'
  | 'QUOTED'
  | 'BIND_REQUESTED'
  | 'BOUND'
  | 'BOOKED'
  | 'DECLINED'
  | 'NOT_TAKEN_UP'
  | 'WITHDRAWN'
  | 'CANCELLED'

export type Action =
  | 'SUBMIT'
  | 'START_REVIEW'
  | 'REQUEST_INFO'
  | 'PROVIDE_INFO'
  | 'QUOTE'
  | 'DECLINE'
  | 'REQUEST_BIND'
  | 'REJECT_QUOTE'
  | 'BIND'
  | 'BOOK'
  | 'WITHDRAW'
  | 'CANCEL'

export type LineOfBusiness =
  | 'PROPERTY'
  | 'GENERAL_LIABILITY'
  | 'PROFESSIONAL_INDEMNITY'
  | 'CYBER'
  | 'MARINE_CARGO'
  | 'DIRECTORS_AND_OFFICERS'

export interface User {
  id: string
  email: string
  fullName: string
  role: Role
  brokerageName: string | null
}

export interface SubmissionDetails {
  lineOfBusiness: LineOfBusiness
  insuredName: string
  insuredCountry: string | null
  insuredIndustry: string | null
  annualRevenue: number | null
  sumInsured: number | null
  currency: string | null
  coverageStart: string | null
  coverageEnd: string | null
  targetPremium: number | null
  riskDescription: string | null
}

export interface Submission extends SubmissionDetails {
  id: string
  reference: string
  status: Status
  quotedPremium: number | null
  quoteValidUntil: string | null
  quoteTerms: string | null
  policyNumber: string | null
  closedReason: string | null
  brokerageName: string
  brokerName: string
  underwriterName: string | null
  createdAt: string
  updatedAt: string
  statusChangedAt: string
  daysInStatus: number
  missingFields: string[]
  availableActions: Action[]
}

export interface SubmissionEvent {
  id: string
  fromStatus: Status | null
  toStatus: Status
  actorName: string | null
  note: string | null
  createdAt: string
}

export interface Message {
  id: string
  authorName: string
  authorRole: Role
  body: string
  createdAt: string
}

export interface SubmissionDetail {
  submission: Submission
  events: SubmissionEvent[]
  messages: Message[]
}

export interface ActionRequest {
  action: Action
  note?: string
  premium?: number
  validUntil?: string
  terms?: string
}

export interface Dashboard {
  countsByStatus: Partial<Record<Status, number>>
  waitingOnMe: Submission[]
  expiringQuotes: Submission[]
  turnaround: {
    submittedToQuotedHours: number | null
    quotedSample: number
    submittedToBoundHours: number | null
    boundSample: number
  }
}
