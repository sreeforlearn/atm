import type { ReactNode } from 'react';

export interface CardProps {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
  padded?: boolean;
}

/**
 * Glass-morphism surface used to wrap content sections.
 *
 * Renders an optional header with title, description and a slot for actions.
 */
export default function Card({
  title,
  description,
  actions,
  children,
  className = '',
  padded = true,
}: CardProps) {
  return (
    <section className={`glass-card ${padded ? 'p-6' : ''} ${className}`} aria-label={title}>
      {(title || actions) && (
        <header className="mb-4 flex items-start justify-between gap-4">
          <div>
            {title && (
              <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
            )}
            {description && (
              <p className="mt-1 text-sm text-slate-500">{description}</p>
            )}
          </div>
          {actions && <div className="shrink-0">{actions}</div>}
        </header>
      )}
      {children}
    </section>
  );
}
