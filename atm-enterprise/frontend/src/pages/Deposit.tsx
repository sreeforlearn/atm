import { useMemo, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Banknote, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { accountService } from '@/services/account.service';
import { useDeposit } from '@/hooks/useTransactions';
import { formatCurrency } from '@/utils/format';
import { isValidAmount } from '@/utils/validation';
import Card from '@/components/ui/Card';
import Modal from '@/components/ui/Modal';
import Loader from '@/components/ui/Loader';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';

type DepositSource = 'CASH' | 'CHEQUE';

/**
 * Deposit page.
 *
 * Customer selects an account, an amount, a source (cash or cheque), and an
 * optional remark. A confirmation modal previews the deposit before it is
 * submitted; success toast + redirect to /transactions.
 */
export default function Deposit() {
  const navigate = useNavigate();
  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: () => accountService.getAccounts(),
  });

  const defaultAccount = useMemo(() => accounts?.[0]?.id ?? '', [accounts]);
  const [accountId, setAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [remark, setRemark] = useState('');
  const [source, setSource] = useState<DepositSource>('CASH');
  const [amountError, setAmountError] = useState<string | undefined>();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const deposit = useDeposit(accountId || defaultAccount);

  const selectedAccount = accounts?.find((a) => a.id === (accountId || defaultAccount));

  const validate = (): boolean => {
    if (!isValidAmount(amount)) {
      setAmountError('Enter a positive amount (max ₹1,00,000).');
      return false;
    }
    setAmountError(undefined);
    return true;
  };

  const handleSubmit = () => {
    if (!accountId) setAccountId(defaultAccount);
    if (!validate()) return;
    setConfirmOpen(true);
  };

  const handleConfirm = () => {
    deposit.mutate(
      {
        accountId: accountId || defaultAccount,
        amount: Number(amount),
        source,
        remark: remark || undefined,
      },
      {
        onSuccess: () => {
          setConfirmOpen(false);
          setAmount('');
          setRemark('');
          navigate('/transactions');
        },
        onError: () => setConfirmOpen(false),
      },
    );
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">Deposit</h1>
        <p className="text-sm text-white/70">
          Add cash or a cheque to your account instantly.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Deposit details" className="lg:col-span-2">
          {isLoading ? (
            <Loader label="Loading accounts…" className="py-10" />
          ) : (
            <div className="space-y-4">
              <div>
                <label htmlFor="account" className="block text-sm font-medium text-slate-700 mb-1.5">
                  Account
                </label>
                <select
                  id="account"
                  name="account"
                  value={accountId || defaultAccount}
                  onChange={(e) => setAccountId(e.target.value)}
                  className="form-input"
                >
                  {accounts?.length === 0 && <option value="">No accounts available</option>}
                  {accounts?.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountType} • {a.accountNumber.slice(-4)}
                    </option>
                  ))}
                </select>
              </div>

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
                error={amountError}
                placeholder="0.00"
                leftIcon={<Banknote className="h-5 w-5" />}
              />

              <fieldset>
                <legend className="block text-sm font-medium text-slate-700 mb-1.5">
                  Source
                </legend>
                <div className="grid grid-cols-2 gap-3">
                  <SourceOption
                    label="Cash"
                    description="Cash deposited at ATM"
                    icon={<Banknote className="h-5 w-5" />}
                    active={source === 'CASH'}
                    onClick={() => setSource('CASH')}
                  />
                  <SourceOption
                    label="Cheque"
                    description="Cheque deposit (clears in 1-2 days)"
                    icon={<FileText className="h-5 w-5" />}
                    active={source === 'CHEQUE'}
                    onClick={() => setSource('CHEQUE')}
                  />
                </div>
              </fieldset>

              <Input
                label="Remark (optional)"
                id="remark"
                name="remark"
                value={remark}
                onChange={(e) => setRemark(e.target.value)}
                maxLength={80}
                placeholder="e.g. Salary deposit"
              />

              <Button type="button" size="lg" className="w-full" onClick={handleSubmit}>
                Continue
              </Button>
            </div>
          )}
        </Card>

        <Card title="Tips" padded>
          <ul className="space-y-2 text-sm text-slate-600">
            <li>Cash deposits reflect immediately on your balance.</li>
            <li>Cheque deposits pass through clearing (typically 1-2 working days).</li>
            <li>Keep your receipt reference for any disputes.</li>
            <li>The ATM accepts up to 200 notes per transaction.</li>
          </ul>
        </Card>
      </div>

      <Modal
        open={confirmOpen}
        title="Confirm deposit"
        description="Verify the deposit details below."
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleConfirm}
        confirmLabel="Deposit"
        loading={deposit.isPending}
      >
        <dl className="space-y-2">
          <div className="flex justify-between">
            <dt className="text-slate-500">Account</dt>
            <dd className="font-mono">{selectedAccount?.accountNumber.slice(-4)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Source</dt>
            <dd>{source === 'CASH' ? 'Cash' : 'Cheque'}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Amount</dt>
            <dd className="font-bold text-success-700">{formatCurrency(Number(amount) || 0)}</dd>
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

function SourceOption({
  label,
  description,
  icon,
  active,
  onClick,
}: {
  label: string;
  description: string;
  icon: ReactNode;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`flex items-start gap-3 rounded-xl border p-3 text-left transition-all ${
        active
          ? 'border-primary-500 bg-primary-50 ring-2 ring-primary-200'
          : 'border-slate-200 hover:border-slate-300'
      }`}
    >
      <span className={active ? 'text-primary-700' : 'text-slate-500'}>{icon}</span>
      <div>
        <p className="text-sm font-semibold text-slate-900">{label}</p>
        <p className="text-xs text-slate-500">{description}</p>
      </div>
    </button>
  );
}
