import { useState, type FormEvent } from 'react';
import EmailInput from './components/EmailInput';
import ToneSelector from './components/ToneSelector';
import ResultPanel from './components/ResultPanel';
import LoadingIndicator from './components/LoadingIndicator';
import { transformEmail } from './api/transformApi';
import type { ToneId, TransformResponse } from './types';

/**
 * Top-level page. Two-column layout on >=md, stacked on mobile.
 *
 * WHY no state-management library: the entire app has 5 pieces of state
 * (email, tone, context, preserveLength, plus result/loading/error). useState
 * is the right tool. Reaching for Zustand/Redux/etc. for this would just
 * obscure the wiring.
 */
export default function App() {
  // -------- Form state --------
  const [email, setEmail] = useState('');
  const [tone, setTone] = useState<ToneId | null>(null);
  const [context, setContext] = useState('');
  const [preserveLength, setPreserveLength] = useState(false);

  // -------- Async state --------
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<TransformResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const canSubmit =
    email.trim().length >= 10 &&
    email.length <= 5000 &&
    tone !== null &&
    !loading;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!canSubmit || tone === null) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const resp = await transformEmail({
        originalEmail: email,
        tone,
        context: context.trim() ? context : undefined,
        preserveLength,
      });
      setResult(resp);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:py-12">
      <header className="mb-8">
        <h1 className="text-3xl font-semibold tracking-tight text-slate-900">
          Email Tone Transformer
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Paste an email, pick a tone, and have it rewritten — preserving your meaning.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 md:gap-8">
        {/* ---------- Left column: input form ---------- */}
        <form onSubmit={onSubmit} className="flex flex-col gap-5">
          <EmailInput value={email} onChange={setEmail} disabled={loading} />

          <ToneSelector selected={tone} onChange={setTone} disabled={loading} />

          <div className="flex flex-col gap-1">
            <label htmlFor="context-input" className="text-sm font-medium text-slate-700">
              Context <span className="font-normal text-slate-500">(optional)</span>
            </label>
            <input
              id="context-input"
              type="text"
              value={context}
              onChange={(e) => setContext(e.target.value)}
              disabled={loading}
              placeholder="Who are you writing to? e.g. 'my manager' or 'an upset customer'"
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm
                         focus:border-accent-500 focus:outline-none focus:ring-2 focus:ring-accent-100
                         disabled:opacity-60 disabled:cursor-not-allowed transition"
            />
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={preserveLength}
              onChange={(e) => setPreserveLength(e.target.checked)}
              disabled={loading}
              className="h-4 w-4 rounded border-slate-300 text-accent-600 focus:ring-accent-500"
            />
            Keep approximately the same length
          </label>

          <button
            type="submit"
            disabled={!canSubmit}
            className="mt-2 inline-flex items-center justify-center rounded-lg bg-accent-600 px-4 py-2.5
                       text-sm font-semibold text-white shadow-sm transition
                       hover:bg-accent-700 disabled:bg-slate-300 disabled:cursor-not-allowed"
          >
            Transform
          </button>

          {error && (
            <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
              {error}
            </div>
          )}
        </form>

        {/* ---------- Right column: result / loading / empty ---------- */}
        <div>
          {loading ? (
            <LoadingIndicator tone={tone} />
          ) : result ? (
            <ResultPanel result={result} />
          ) : (
            <div className="rounded-lg border border-dashed border-slate-300 bg-white/60 p-10 text-center">
              <h2 className="text-base font-medium text-slate-700">Your rewrite will appear here</h2>
              <p className="mt-2 text-sm text-slate-500">
                Pick a tone on the left, write or paste an email, then hit Transform.
              </p>
            </div>
          )}
        </div>
      </div>

      <footer className="mt-12 border-t border-slate-200 pt-4 text-xs text-slate-500">
        Learning reference for Spring AI 1.0.x · gpt-4o-mini · ~$0.0003 per transform
      </footer>
    </div>
  );
}
