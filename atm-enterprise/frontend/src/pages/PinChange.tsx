import { useState } from 'react';
import { KeyRound, Lock, ShieldCheck } from 'lucide-react';
import { toast } from 'react-toastify';
import { useAuthStore } from '@/store/authStore';
import { useChangePin } from '@/hooks/useAuth';
import { isWeakPin, isValidPin, pinsMatch } from '@/utils/validation';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Card from '@/components/ui/Card';
import { formatStrength } from './PinChange.helpers';

/**
 * PIN change page — three inputs:
 *  1. Old PIN (must match current PIN server-side)
 *  2. New PIN (4 digits, not weak/sequential)
 *  3. Confirm new PIN (must match new PIN)
 *
 * On success, a toast shows and the form resets.
 */
export default function PinChange() {
  const card = useAuthStore((s) => s.card);
  const changePin = useChangePin();

  const [oldPin, setOldPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [errors, setErrors] = useState<{ old?: string; new?: string; confirm?: string }>({});

  const validate = (): boolean => {
    const next: typeof errors = {};
    if (!isValidPin(oldPin)) next.old = 'Enter your current 4-digit PIN.';
    if (!isValidPin(newPin)) {
      next.new = 'New PIN must be 4 digits.';
    } else if (isWeakPin(newPin)) {
      next.new = 'Avoid sequential or repeated PINs (e.g. 1234, 1111).';
    } else if (oldPin === newPin) {
      next.new = 'New PIN must differ from the current PIN.';
    }
    if (!pinsMatch(newPin, confirmPin)) {
      next.confirm = 'PINs do not match.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = () => {
    if (!card?.id) {
      toast.error('No card linked to your session.');
      return;
    }
    if (!validate()) return;
    changePin.mutate(
      { cardId: card.id, oldPin, newPin },
      {
        onSuccess: () => {
          setOldPin('');
          setNewPin('');
          setConfirmPin('');
        },
      },
    );
  };

  const strength = formatStrength(newPin);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">Change ATM PIN</h1>
        <p className="text-sm text-white/70">
          Pick a 4-digit PIN that's easy for you to remember and hard for others to guess.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Update PIN" className="lg:col-span-2">
          <div className="space-y-5">
            <Input
              label="Current PIN"
              id="oldPin"
              name="oldPin"
              type="password"
              inputMode="numeric"
              maxLength={4}
              value={oldPin}
              onChange={(e) => setOldPin(e.target.value.replace(/\D/g, ''))}
              error={errors.old}
              leftIcon={<KeyRound className="h-5 w-5" />}
              autoComplete="off"
            />
            <div>
              <Input
                label="New PIN"
                id="newPin"
                name="newPin"
                type="password"
                inputMode="numeric"
                maxLength={4}
                value={newPin}
                onChange={(e) => setNewPin(e.target.value.replace(/\D/g, ''))}
                error={errors.new}
                leftIcon={<Lock className="h-5 w-5" />}
                autoComplete="off"
              />
              <div className="mt-2 flex items-center gap-2">
                <div className="h-1.5 flex-1 rounded-full bg-slate-200 overflow-hidden">
                  <div
                    className={`h-full transition-all ${strength.className}`}
                    style={{ width: `${strength.pct}%` }}
                  />
                </div>
                <span className="text-xs text-slate-500">{strength.label}</span>
              </div>
            </div>
            <Input
              label="Confirm new PIN"
              id="confirmPin"
              name="confirmPin"
              type="password"
              inputMode="numeric"
              maxLength={4}
              value={confirmPin}
              onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, ''))}
              error={errors.confirm}
              leftIcon={<ShieldCheck className="h-5 w-5" />}
              autoComplete="off"
            />

            <Button
              type="button"
              className="w-full"
              size="lg"
              loading={changePin.isPending}
              onClick={handleSubmit}
            >
              Update PIN
            </Button>
          </div>
        </Card>

        <Card title="PIN safety" padded>
          <ul className="space-y-2 text-sm text-slate-600">
            <li>Use 4 digits (numeric only).</li>
            <li>Avoid your birth year, phone suffix or repeating digits.</li>
            <li>Never write your PIN on your card or share it with anyone.</li>
            <li>Change your PIN at least once every 6 months.</li>
            <li>The new PIN takes effect immediately for all ATM sessions.</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
