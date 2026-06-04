import axios, { AxiosError } from 'axios';
import type {
  ProblemDetail,
  ToneInfo,
  TransformRequest,
  TransformResponse,
} from '../types';

// VITE_API_BASE_URL is read at build time. Falls back to localhost:8080 so
// `npm run dev` works with no .env file.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const http = axios.create({
  baseURL,
  // 30s — LLM calls are slow. The backend's @Retryable can take up to ~6s
  // by itself on bad luck, plus the actual model latency.
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Translates an axios error into a plain Error with a user-friendly message.
 *
 * WHY: the React tree just throws/catches Error. Centralizing the
 * RFC-7807-aware translation means components don't each have to special-case
 * problem+json shapes.
 */
function userFriendlyError(err: unknown): Error {
  const axiosErr = err as AxiosError<ProblemDetail>;
  const data = axiosErr.response?.data;
  if (data && (data.detail || data.title)) {
    return new Error(data.detail ?? data.title ?? 'Unexpected error');
  }
  if (axiosErr.code === 'ECONNABORTED') {
    return new Error('Request timed out. The AI took too long to respond — try again.');
  }
  if (axiosErr.message) return new Error(axiosErr.message);
  return new Error('Unexpected error');
}

export async function fetchTones(): Promise<ToneInfo[]> {
  try {
    const res = await http.get<ToneInfo[]>('/api/tones');
    return res.data;
  } catch (e) {
    throw userFriendlyError(e);
  }
}

export async function transformEmail(req: TransformRequest): Promise<TransformResponse> {
  try {
    const res = await http.post<TransformResponse>('/api/transform', req);
    return res.data;
  } catch (e) {
    throw userFriendlyError(e);
  }
}
