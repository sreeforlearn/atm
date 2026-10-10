/**
 * Tiny helper that derives a strength bar class + percentage for the
 * PinChange page. Kept in its own module to keep the page readable.
 */
export interface PinStrength {
  pct: number;
  label: string;
  className: string;
}

const WEAK: PinStrength = { pct: 25, label: 'Weak', className: 'bg-danger-500' };
const MEDIUM: PinStrength = { pct: 60, label: 'Fair', className: 'bg-accent-500' };
const STRONG: PinStrength = { pct: 100, label: 'Strong', className: 'bg-success-500' };

export function formatStrength(pin: string): PinStrength {
  if (!pin) return { pct: 0, label: '—', className: 'bg-slate-300' };
  // Very simple heuristic — purely for visual feedback, not security.
  const unique = new Set(pin).size;
  if (unique <= 1) return WEAK;
  if (unique === 2) return MEDIUM;
  return STRONG;
}
