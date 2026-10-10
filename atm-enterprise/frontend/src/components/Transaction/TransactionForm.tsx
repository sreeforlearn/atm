import { type FormEvent, type ReactNode } from 'react';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import type { Account, Option } from '@/types';

export interface TransactionFormProps {
  /** Pre-fill account dropdown with the user's accounts. */
  accounts: Account[];
  selectedAccountId: string;
  onAccountChange: (id: string) => void;
  amount: string;
  onAmountChange: (amount: string) => void;
  amountError?: string;
  remark?: string;
  onRemarkChange?: (remark: string) => void;
  pin?: string;
  onPinChange?: (pin: string) => void;
  pinError?: string;
  onSubmit: () => void;
  submitLabel?: string;
  loading?: boolean;
  /** Optional children slot for extra form fields (e.g. to-account). */
  children?: ReactNode;
  /** Render a row of quick amount chips below the amount input. */
  quickAmounts?: number[];
  onQuickAmount?: (amt: number) => void;
}

const accountOptions = (accounts: Account[]): Option<string>[] =>
  accounts.map((a) => ({
    label: `${a.accountType} • ${a.accountNumber.slice(-4)}`,
    value: a.id,
  }));

/**
 * Reusable transaction form shell: account select, amount input with quick
 * chips, optional remark, optional PIN (for transfers/withdraws), and a submit
 * button. Specific pages compose extra fields via children.
 */
export default function TransactionForm({
  accounts,
  selectedAccountId,
  onAccountChange,
  amount,
  onAmountChange,
  amountError,
  remark,
  onRemarkChange,
  pin,
  onPinChange,
  pinError,
  onSubmit,
  submitLabel = 'Continue',
  loading = false,
  children,
  quickAmounts,
  onQuickAmount,
}: TransactionFormProps) {
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="account" className="block text-sm font-medium text-slate-700 mb-1.5">
          Account
        </label>
        <select
          id="account"
          name="account"
          value={selectedAccountId}
          onChange={(e) => onAccountChange(e.target.value)}
          className="form-input"
        >
          {accounts.length === 0 && <option value="">No accounts available</option>}
          {accountOptions(accounts).map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <Input
        label="Amount (₹)"
        id="amount"
        name="amount"
        inputMode="decimal"
        type="number"
        min={0}
        step="0.01"
        value={amount}
        onChange={(e) => onAmountChange(e.target.value)}
        error={amountError}
        placeholder="0.00"
        autoComplete="off"
      />

      {quickAmounts && quickAmounts.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {quickAmounts.map((amt) => (
            <button
              key={amt}
              type="button"
              onClick={() => onQuickAmount?.(amt)}
              className="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700 hover:bg-slate-200"
            >
              ₹{amt.toLocaleString('en-IN')}
            </button>
          ))}
        </div>
      )}

      {children}

      {onRemarkChange && (
        <Input
          label="Remark (optional)"
          id="remark"
          name="remark"
          value={remark ?? ''}
          onChange={(e) => onRemarkChange(e.target.value)}
          placeholder="e.g. Monthly rent"
          maxLength={80}
        />
      )}

      {onPinChange && (
        <Input
          label="ATM PIN"
          id="pin"
          name="pin"
          type="password"
          inputMode="numeric"
          maxLength={4}
          value={pin ?? ''}
          onChange={(e) => onPinChange(e.target.value)}
          error={pinError}
          hint="4-digit ATM PIN"
          autoComplete="off"
        />
      )}

      <Button type="submit" loading={loading} className="w-full" size="lg">
        {submitLabel}
      </Button>
    </form>
  );
}
