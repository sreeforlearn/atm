import { type FormEvent, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { CreditCard, KeyRound, Lock, ShieldCheck, User } from 'lucide-react';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { useLogin } from '@/hooks/useAuth';
import { isValidCardNumber, isValidPin } from '@/utils/validation';
import { maskCardNumber } from '@/utils/format';
interface LocationState {
  from?: string;
}

/**
 * Login page — captures the customer's 16-digit card number + 4-digit PIN,
 * validates client-side, then calls the `useLogin` mutation. On success the
 * mutation navigates to `/dashboard` (or the original `from` route).
 */
export default function Login() {
  const [cardNumber, setCardNumber] = useState('');
  const [pin, setPin] = useState('');
  const [errors, setErrors] = useState<{ cardNumber?: string; pin?: string }>({});
  const login = useLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as LocationState | null)?.from ?? '/dashboard';

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const nextErrors: typeof errors = {};
    if (!isValidCardNumber(cardNumber)) {
      nextErrors.cardNumber = 'Enter a valid 16-digit card number.';
    }
    if (!isValidPin(pin)) {
      nextErrors.pin = 'PIN must be 4 digits.';
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    login.mutate({ cardNumber: cardNumber.replace(/\D/g, ''), pin }, {
      onSuccess: () => navigate(from, { replace: true }),
    });
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center text-white">
          <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-accent-500 text-2xl font-bold text-primary-950 shadow-atm">
            ATM
          </div>
          <h1 className="text-3xl font-bold tracking-tight">ATM Enterprise</h1>
          <p className="mt-2 text-sm text-white/70">
            Secure digital banking &amp; cashpoint access
          </p>
        </div>

        <form onSubmit={onSubmit} className="glass-card p-8 space-y-5 animate-fade-in">
          <div className="flex items-center gap-2 text-sm text-slate-600 bg-slate-50 border border-slate-200 rounded-lg px-3 py-2">
            <ShieldCheck className="h-4 w-4 text-success-600" />
            End-to-end TLS secured session
          </div>

          <Input
            label="Card Number"
            id="cardNumber"
            name="cardNumber"
            inputMode="numeric"
            autoComplete="username"
            placeholder="1234 5678 9012 3456"
            value={cardNumber}
            onChange={(e) => setCardNumber(e.target.value)}
            error={errors.cardNumber}
            leftIcon={<CreditCard className="h-5 w-5" />}
            maxLength={19}
          />

          <Input
            label="ATM PIN"
            id="pin"
            name="pin"
            type="password"
            inputMode="numeric"
            autoComplete="current-password"
            placeholder="••••"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            error={errors.pin}
            leftIcon={<KeyRound className="h-5 w-5" />}
            maxLength={4}
          />

          <Button
            type="submit"
            size="lg"
            className="w-full"
            loading={login.isPending}
            leftIcon={<Lock className="h-4 w-4" />}
          >
            Sign In Securely
          </Button>

          <div className="text-center text-xs text-slate-500">
            <p className="flex items-center justify-center gap-1">
              <User className="h-3 w-3" /> Demo credentials
            </p>
            <p className="mt-1">
              Card: <span className="font-mono">{maskCardNumber('4111111111111111')}</span> ·
              PIN: <span className="font-mono">1234</span>
            </p>
          </div>
        </form>

        <p className="mt-6 text-center text-xs text-white/60">
          Never share your card or PIN with anyone. Bank staff will never ask for it.
        </p>
      </div>
    </div>
  );
}
