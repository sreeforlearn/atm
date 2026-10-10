import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: ReactNode;
  rightSlot?: ReactNode;
  hint?: string;
}

/**
 * Accessible labelled text input with optional left icon and error display.
 *
 * The `label` is required by the ATM design system for accessibility — pass
 * `aria-label` instead if you need an unlabeled input.
 */
const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    label,
    error,
    leftIcon,
    rightSlot,
    hint,
    id,
    name,
    className,
    ...rest
  },
  ref,
) {
  const inputId = id ?? name;
  const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;

  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}

      <div className="relative">
        {leftIcon && (
          <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
            {leftIcon}
          </span>
        )}
        <input
          ref={ref}
          id={inputId}
          name={name}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          className={`form-input ${leftIcon ? 'pl-10' : ''} ${
            rightSlot ? 'pr-24' : ''
          } ${error ? 'border-danger-400 focus:border-danger-500 focus:ring-danger-100' : ''} ${className ?? ''}`}
          {...rest}
        />
        {rightSlot && (
          <span className="absolute inset-y-0 right-0 flex items-center pr-2">
            {rightSlot}
          </span>
        )}
      </div>

      {error ? (
        <p id={`${inputId}-error`} className="text-xs text-danger-600">
          {error}
        </p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="text-xs text-slate-500">
          {hint}
        </p>
      ) : null}
    </div>
  );
});

export default Input;
