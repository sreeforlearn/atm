import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Banknote } from 'lucide-react';
import { accountService } from '@/services/account.service';
import { useWithdraw } from '@/hooks/useTransactions';
import { formatCurrency } from '@/utils/format';
import { isValidAmount, isValidPin } from '@/utils/validation';
import Card from '@/components/ui/Card';
import TransactionForm from '@/components/Transaction/TransactionForm';
import Modal from '@/components/ui/Modal';
import Loader from '@/components/ui/Loader';
import { useNavigate } from 'react-router-dom';

const QUICK_AMOUNTS = [500, 1000, 2000, 5000, 10000];

/**
 * Withdraw page.
 *
 * - Quick-amount chips (₹500–₹10,000) plus a custom amount field
 * - Account dropdown (defaults to the card-linked account)
 * - ATM PIN confirmation
 * - Confirmation modal summarising the request before submitting.
 */
export default function Withdraw() {
  const navigate = useNavigate();
  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: () => accountService.getAccounts(),
  });

  const defaultAccount = useMemo(
    () => accounts?.[0]?.id ?? '',
    [accounts],
  );

  const [accountId, setAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [pin, setPin] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [errors, setErrors] = useState<{ amount?: string; pin?: string }>({});

  const withdraw = useWithdraw(accountId);

  const selectedAccount = accounts?.find((a) => a.id === (accountId || defaultAccount));

  const validate = (): boolean => {
    const next: typeof errors = {};
    if (!isValidAmount(amount)) {
      next.amount = 'Enter a positive amount (max ₹1,00,000).';
    } else if (selectedAccount && Number(amount) > (selectedAccount.availableBalance ?? selectedAccount.balance)) {
      next.amount = 'Amount exceeds available balance.';
    }
    if (!isValidPin(pin)) {
      next.pin = 'PIN must be 4 digits.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = () => {
    if (!accountId) {
      setAccountId(defaultAccount);
    }
    if (!validate()) return;
    setConfirmOpen(true);
  };

  const handleConfirm = () => {
    withdraw.mutate(
      {
        accountId: accountId || defaultAccount,
        amount: Number(amount),
        pin,
      },
      {
        onSuccess: () => {
          setConfirmOpen(false);
          setAmount('');
          setPin('');
          navigate('/transactions');
        },
        onError: () => setConfirmOpen(false),
      },
    );
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">Withdraw Cash</h1>
        <p className="text-sm text-white/70">
          Pick a quick amount or enter a custom value. Your PIN confirms the withdrawal.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Withdrawal details" className="lg:col-span-2">
          {isLoading ? (
            <Loader label="Loading accounts…" className="py-10" />
          ) : (
            <TransactionForm
              accounts={accounts ?? []}
              selectedAccountId={accountId || defaultAccount}
              onAccountChange={setAccountId}
              amount={amount}
              onAmountChange={setAmount}
              amountError={errors.amount}
              pin={pin}
              onPinChange={setPin}
              pinError={errors.pin}
              quickAmounts={QUICK_AMOUNTS}
              onQuickAmount={(amt) => setAmount(String(amt))}
              submitLabel="Continue"
              loading={withdraw.isPending}
              onSubmit={handleSubmit}
            />
          )}
        </Card>

        <Card title="Limits &amp; tips" padded>
          <ul className="space-y-2 text-sm text-slate-600">
            <li className="flex items-start gap-2">
              <Banknote className="h-5 w-5 text-primary-700" />
              Daily ATM withdrawal limit is ₹50,000.
            </li>
            <li className="pl-7">Min ₹100, max ₹10,000 per transaction at the ATM.</li>
            <li className="pl-7">Cash dispensed in ₹100 / ₹200 / ₹500 denominations.</li>
            <li className="pl-7">PIN is verified server-side before cash is authorised.</li>
          </ul>
        </Card>
      </div>

      <Modal
        open={confirmOpen}
        title="Confirm withdrawal"
        description="Review and confirm the details below."
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleConfirm}
        confirmLabel="Withdraw cash"
        confirmVariant="danger"
        loading={withdraw.isPending}
      >
        <dl className="space-y-2">
          <div className="flex justify-between">
            <dt className="text-slate-500">Account</dt>
            <dd className="font-mono">{selectedAccount?.accountNumber.slice(-4)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Amount</dt>
            <dd className="font-bold text-danger-700">{formatCurrency(Number(amount) || 0)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Available after</dt>
            <dd className="font-semibold">
              {formatCurrency(
                (selectedAccount?.availableBalance ?? 0) - (Number(amount) || 0),
              )}
            </dd>
          </div>
        </dl>
        <p className="mt-4 text-xs text-slate-500">
          By confirming you authorise the ATM network to dispense cash for the amount shown.
        </p>
      </Modal>
    </div>
  );
}
