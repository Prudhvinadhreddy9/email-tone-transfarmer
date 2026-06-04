// Wire types — kept in sync with the backend DTOs.
//
// WHY duplicate the shapes by hand instead of generating from OpenAPI: this is
// a learning project; the DTOs are tiny and the duplication keeps the example
// readable. For a real project, generate this file from the backend's OpenAPI
// document so backend changes break the frontend build immediately.

/** Mirrors backend Tone enum constants. */
export type ToneId =
  | 'PROFESSIONAL'
  | 'FRIENDLY'
  | 'ASSERTIVE'
  | 'APOLOGETIC'
  | 'CONCISE'
  | 'DIPLOMATIC';

/** What GET /api/tones returns per tone. aiInstruction is intentionally absent — see ToneInfo.java. */
export interface ToneInfo {
  id: ToneId;
  displayName: string;
  description: string;
}

export interface TransformRequest {
  originalEmail: string;
  tone: ToneId;
  context?: string;
  preserveLength?: boolean;
}

export interface TokenUsage {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCostUsd: number;
}

export interface TransformResponse {
  transformId: string;
  originalEmail: string;
  transformedEmail: string;
  tone: ToneId;
  changesNotes: string[];
  tokenUsage: TokenUsage;
}

/** RFC 7807 Problem Details — what the GlobalExceptionHandler emits on errors. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}
