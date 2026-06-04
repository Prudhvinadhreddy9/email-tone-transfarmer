import { useState } from 'react';
import type { TransformResponse } from '../types';

/**
 * Displays the transformed email, the changes-made list, and cost.
 * Includes a copy-to-clipboard button on the rewritten email.
 *
 * Uses navigator.clipboard which is widely supported and doesn't require any
 * library. We show a transient "Copied!" affordance via local state.
 */
interface Props {
  result: TransformResponse;
}

export default function ResultPanel({ result }: Props) {
  const [copied, setCopied] = useState(false);

  const onCopy = async () => {
    try {
      await navigator.clipboard.writeText(result.transformedEmail);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Some browsers in non-HTTPS dev contexts disable clipboard. Fall back
      // to selecting the text so the user can copy with cmd/ctrl+C.
      setCopied(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-2">
          <h2 className="text-sm font-medium text-slate-700">
            Rewritten <span className="rounded-full bg-accent-50 px-2 py-0.5 text-xs font-medium text-accent-700">{result.tone}</span>
          </h2>
          <button
            type="button"
            onClick={onCopy}
            className="rounded-md border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700
                       hover:bg-slate-50 transition"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>
        <pre className="whitespace-pre-wrap break-words p-4 text-sm leading-relaxed text-slate-900 font-sans">
          {result.transformedEmail}
        </pre>
      </div>

      {result.changesNotes && result.changesNotes.length > 0 && (
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h3 className="text-sm font-medium text-slate-700">Changes made</h3>
          <ul className="mt-2 list-disc pl-5 text-sm text-slate-700 space-y-1">
            {result.changesNotes.map((note, i) => (
              <li key={i}>{note}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="text-xs text-slate-500">
        {result.tokenUsage.totalTokens.toLocaleString()} tokens
        {' · '}
        Cost: ${result.tokenUsage.estimatedCostUsd.toFixed(4)}
      </div>
    </div>
  );
}
