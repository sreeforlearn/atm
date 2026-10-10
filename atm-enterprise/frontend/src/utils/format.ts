/**
 * Money, date and number formatting helpers for the ATM UI.
 *
 * Currency is hard-coded to INR (₹) — the product target market is Indian retail banking.
 */

const INR = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/**
 * Format a number as an Indian Rupee currency string, e.g. `₹12,345.00`.
 */
export function formatCurrency(amount: number | null | undefined): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) {
    return '₹0.00';
  }
  return INR.format(amount);
}

const dateFmtShort = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});
const dateFmtLong = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/**
 * Short human-readable date, e.g. `04 Aug 2024`.
 */
export function formatDate(value: string | Date | undefined): string {
  if (!value) return '—';
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '—';
  return dateFmtShort.format(d);
}

/**
 * Long date with time, e.g. `04 August 2024, 03:45 PM`.
 */
export function formatDateTime(value: string | Date | undefined): string {
  if (!value) return '—';
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '—';
  return dateFmtLong.format(d);
}

/**
 * Mask an account number — show only the last 4 digits: `**** **** 5678`.
 */
export function maskAccountNumber(accountNumber: string | undefined): string {
  if (!accountNumber) return '****';
  const digits = accountNumber.replace(/\D/g, '');
  if (digits.length < 4) return '****';
  const last = digits.slice(-4);
  return `**** **** ${last}`;
}

/**
 * Mask a 16-digit card number in 4-4-4-4 groups: `4111 **** **** 1111`.
 */
export function maskCardNumber(cardNumber: string | undefined): string {
  if (!cardNumber) return '—';
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length !== 16) return maskAccountNumber(cardNumber);
  return `${digits.slice(0, 4)} **** **** ${digits.slice(-4)}`;
}

/**
 * Convert an ISO date to the `YYYY-MM-DD` value required by `<input type="date">`.
 */
export function toDateInputValue(value: string | Date | undefined): string {
  if (!value) return '';
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '';
  return d.toISOString().slice(0, 10);
}
