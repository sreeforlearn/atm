import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Send, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { accountService } from '@/services/account.service';
import { useTransfer } from '@/hooks/useTransactions';
import { formatCurrency } from '@/utils/format';
import { isValidAmount, isValidPin } from '@/utils/validation';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Modal from '@/components/ui/Modal';
import Loader from '@/components/ui/Loader';
import Button from '@/components/ui/Button';

/**
 * Transfer page.
 *
 * Captures `from`, `to`, amount, remark and PIN, then opens a confirmation
 * modal that summarises the transfer before submission.
 */
export default function Transfer() {
  const navigate = useNavigate();
  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: () => accountService.getAccounts(),
  });

  const defaultAccount = useMemo(() => accounts?.[0]?.id ?? '', [accounts]);

  const [fromAccountId, setFromAccountId] = useState('');
  const [toAccountNumber, setToAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [remark, setRemark] = useState('');
  const [pin, setPin] = useState('');
  const [errors, setErrors] = useState<{
    toAccount?: string;
    amount?: string;
    pin?: string;
  }>({});
  const [confirmOpen, setConfirmOpen] = useState(false);

  const transfer = useTransfer(fromAccountId || defaultAccount);

  const fromAccount = accounts?.find((a) => a.id === (fromAccountId || defaultAccount));

  const validate = (): boolean => {
    const next: typeof errors = {};
    if (toAccountNumber.trim().length < 8) {
      next.toAccount = 'Enter a valid destination account number.';
    }
    if (!isValidAmount(amount)) {
      next.amount = 'Enter a positive amount (max ₹1,00,000).';
    } else if (fromAccount && Number(amount) > (fromAccount.availableBalance ?? fromAccount.balance)) {
      next.amount = 'Amount exceeds available balance.';
    }
    if (!isValidPin(pin)) {
      next.pin = 'PIN must be 4 digits.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = () => {
    if (!fromAccountId) setFromAccountId(defaultAccount);
    if (!validate()) return;
    setConfirmOpen(true);
  };

  const handleConfirm = () => {
    transfer.mutate(
      {
        fromAccountId: fromAccountId || defaultAccount,
        toAccountNumber: toAccountNumber.trim(),
        amount: Number(amount),
        remark: remark || undefined,
        pin,
      },
      {
        onSuccess: () => {
          setConfirmOpen(false);
          setToAccountNumber('');
          setAmount('');
          setRemark('');
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
        <h1 className="text-2xl font-bold text-white">Transfer Money</h1>
        <p className="text-sm text-white/70">
          Move funds from your account to another account at the same bank.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Transfer details" className="lg:col-span-2">
          {isLoading ? (
            <Loader label="Loading accounts…" className="py-10" />
          ) : (
            <div className="space-y-4">
              <div>
                <label htmlFor="fromAccount" className="block text-sm font-medium text-slate-700 mb-1.5">
                  From account
                </label>
                <select
                  id="fromAccount"
                  name="fromAccount"
                  value={fromAccountId || defaultAccount}
                  onChange={(e) => setFromAccountId(e.target.value)}
                  className="form-input"
                >
                  {accounts?.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountType} • {a.accountNumber.slice(-4)} ·{' '}
                      {formatCurrency(a.availableBalance ?? a.balance)}
                    </option>
                  ))}
                </select>
              </div>

              <Input
                label="To account number"
                id="toAccount"
                name="toAccount"
                value={toAccountNumber}
                onChange={(e) => setToAccountNumber(e.target.value)}
                error={errors.toAccount}
                leftIcon={<ArrowRight className="h-5 w-5" />}
                placeholder="Beneficiary account number"
                inputMode="numeric"
              />

              <Input
                label="Amount (₹)"
                id="amount"
                name="amount"
                type="number"
                inputMode="decimal"
                min={0}
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                error={errors.amount}
                placeholder="0.00"
              />

              <Input
                label="Remark (optional)"
                id="remark"
                name="remark"
                value={remark}
                onChange={(e) => setRemark(e.target.value)}
                maxLength={80}
                placeholder="e.g. Loan EMI"
              />

              <Input
                label="ATM PIN"
                id="pin"
                name="pin"
                type="password"
                inputMode="numeric"
                maxLength={4}
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                error={errors.pin}
                hint="Confirms the transaction"
              />

              <Button
                type="button"
                size="lg"
                className="w-full"
                onClick={handleSubmit}
                leftIcon={<Send className="h-4 w-4" />}
              >
                Continue
              </Button>
            </div>
          )}
        </Card>

        <Card title="Heads up" padded>
          <ul className="space-y-2 text-sm text-slate-600">
            <li>Transfers between your accounts are instant.</li>
            <li>IMPS / NEFT to other banks follow the network's normal cut-off times.</li>
            <li>You can transfer up to ₹1,00,000 per transaction.</li>
            <li>Beneficiary name must match the destination account holder's name.</li>
            <li>Never transfer to unknown accounts — your bank cannot reverse fraud.</li>
          </ul>
        </Card>
      </div>

      <Modal
        open={confirmOpen}
        title="Confirm transfer"
        description="Once confirmed, the funds leave your account immediately."
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleConfirm}
        confirmLabel="Transfer money"
        confirmVariant="primary"
        loading={transfer.isPending}
      >
        <dl className="space-y-2">
          <div className="flex justify-between">
            <dt className="text-slate-500">From</dt>
            <dd className="font-mono">{fromAccount?.accountNumber.slice(-4)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">To</dt>
            <dd className="font-mono">{toAccountNumber}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Amount</dt>
            <dd className="font-bold text-primary-700">{formatCurrency(Number(amount) || 0)}</dd>
          </div>
          {remark && (
            <div className="flex justify-between">
              <dt className="text-slate-500">Remark</dt>
              <dd>{remark}</dd>
            </div>
          )}
        </dl>
      </Modal>
    </div>
  );
}
