import { useEffect, useState } from 'react';
import type { ToneId } from '../types';

/**
 * Spinner + rotating "what we're doing" messages.
 *
 * WHY rotating messages: LLM calls take 1-5+ seconds, which is long enough
 * for users to wonder if the page is broken. A subtle "Choosing better
 * words..." reassures them that work is happening, without being condescending.
 *
 * The messages are intentionally vague — they don't claim to know the model's
 * internal state, they just give the user something to read while waiting.
 */
const STAGES = [
  'Reading your email...',
  'Choosing better words...',
  'Polishing the tone...',
];

interface Props {
  tone: ToneId | null;
}

export default function LoadingIndicator({ tone }: Props) {
  const [stageIdx, setStageIdx] = useState(0);

  useEffect(() => {
    const id = setInterval(() => {
      setStageIdx((i) => (i + 1) % STAGES.length);
    }, 1200);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-accent-600" />
      <div className="text-sm font-medium text-slate-700">
        {tone ? `Rewriting in ${tone.toLowerCase()} tone...` : 'Working on it...'}
      </div>
      <div className="text-xs text-slate-500">{STAGES[stageIdx]}</div>
    </div>
  );
}
