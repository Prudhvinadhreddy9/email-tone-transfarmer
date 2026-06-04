import { useEffect, useState } from 'react';
import { fetchTones } from '../api/transformApi';
import type { ToneId, ToneInfo } from '../types';

/**
 * Segmented button group. Fetches tones from GET /api/tones once on mount.
 *
 * WHY fetch instead of hardcoding: when we add or rename a tone server-side,
 * the UI updates without a frontend deploy. The endpoint exists for exactly
 * this reason — keep one source of truth.
 *
 * Each button shows the displayName; the description appears as a native
 * tooltip via the title attribute. For a richer tooltip you'd reach for a
 * library, but `title=` is free and accessible.
 */
interface Props {
  selected: ToneId | null;
  onChange: (tone: ToneId) => void;
  disabled?: boolean;
}

export default function ToneSelector({ selected, onChange, disabled }: Props) {
  const [tones, setTones] = useState<ToneInfo[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchTones()
      .then((list) => {
        if (!cancelled) setTones(list);
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <div className="rounded-md border border-red-200 bg-red-50 p-2 text-xs text-red-800">
        Could not load tones: {error}
      </div>
    );
  }

  if (tones.length === 0) {
    return <div className="text-xs text-slate-500">Loading tones...</div>;
  }

  return (
    <div className="flex flex-col gap-1">
      <span className="text-sm font-medium text-slate-700">Target tone</span>
      <div role="radiogroup" className="flex flex-wrap gap-2">
        {tones.map((t) => {
          const isSelected = t.id === selected;
          return (
            <button
              key={t.id}
              type="button"
              role="radio"
              aria-checked={isSelected}
              title={t.description}
              disabled={disabled}
              onClick={() => onChange(t.id)}
              className={[
                'rounded-full border px-3 py-1.5 text-sm transition',
                'disabled:opacity-60 disabled:cursor-not-allowed',
                isSelected
                  ? 'border-accent-600 bg-accent-600 text-white shadow-sm'
                  : 'border-slate-300 bg-white text-slate-700 hover:border-accent-500 hover:text-accent-700',
              ].join(' ')}
            >
              {t.displayName}
            </button>
          );
        })}
      </div>
    </div>
  );
}
