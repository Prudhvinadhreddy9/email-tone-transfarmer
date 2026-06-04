import { type ChangeEvent } from 'react';

/**
 * Controlled textarea for the original email + a live character counter.
 * Highlights when the user is below min or above max length.
 */
interface Props {
  value: string;
  onChange: (next: string) => void;
  min?: number;
  max?: number;
  disabled?: boolean;
}

export default function EmailInput({ value, onChange, min = 10, max = 5000, disabled }: Props) {
  const len = value.length;
  const tooShort = len > 0 && len < min;
  const tooLong = len > max;
  const counterColor = tooShort || tooLong ? 'text-red-600' : 'text-slate-500';

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor="email-input" className="text-sm font-medium text-slate-700">
        Your email draft
      </label>
      <textarea
        id="email-input"
        value={value}
        onChange={(e: ChangeEvent<HTMLTextAreaElement>) => onChange(e.target.value)}
        disabled={disabled}
        rows={10}
        placeholder="Paste or type the email you want rewritten..."
        className="w-full rounded-lg border border-slate-300 bg-white p-3 text-sm leading-relaxed shadow-sm
                   focus:border-accent-500 focus:outline-none focus:ring-2 focus:ring-accent-100
                   disabled:opacity-60 disabled:cursor-not-allowed
                   transition"
      />
      <div className="flex justify-between text-xs">
        <span className={counterColor}>
          {tooShort && `Need at least ${min - len} more character${min - len === 1 ? '' : 's'}`}
          {tooLong && `${len - max} characters over the limit`}
        </span>
        <span className={counterColor}>
          {len.toLocaleString()} / {max.toLocaleString()}
        </span>
      </div>
    </div>
  );
}
