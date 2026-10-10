import { Loader2 } from 'lucide-react';

export interface LoaderProps {
  /** Full-screen loader used by Suspense fallback and page-level fetching. */
  full?: boolean;
  label?: string;
  className?: string;
}

/**
 * Spinner component. Used standalone or as Suspense fallback.
 */
export default function Loader({ full, label, className = '' }: LoaderProps) {
  const inner = (
    <div className={`flex flex-col items-center gap-3 ${className}`}>
      <Loader2 className="h-8 w-8 animate-spin text-primary-700" />
      {label && <p className="text-sm text-slate-500">{label}</p>}
    </div>
  );

  if (full) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        {inner}
      </div>
    );
  }
  return inner;
}
