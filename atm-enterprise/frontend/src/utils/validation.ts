/**
 * Lightweight validation helpers for the ATM UI.
 *
 * These are intentionally simple (no external dep). The server is the source
 * of truth — these functions only give instant client feedback.
 */

/**
 * PIN must be 4 digits (numeric only) for the demo system.
 */
export function isValidPin(pin: string): boolean {
  return /^\d{4}$/.test(pin ?? '');
}

/**
 * Amount is valid when it is a positive number with at most 2 decimal places.
 * Optionally enforce an upper bound (e.g. ATM daily limit of ₹50,000).
 */
export function isValidAmount(amount: number | string, max = 1_000_000): boolean {
  const n = typeof amount === 'string' ? Number(amount) : amount;
  if (!Number.isFinite(n) || n <= 0) return false;
  // Reject if it has more than 2 decimal places.
  if (Math.round(n * 100) !== n * 100) return false;
  return n <= max;
}

/**
 * Luhn checksum check on a 16-digit card number.
 * Returns `true` only when the number is well-formed AND passes the Luhn check.
 */
export function isValidCardNumber(cardNumber: string): boolean {
  const digits = (cardNumber ?? '').replace(/\D/g, '');
  if (digits.length < 13 || digits.length > 19) return false;

  let sum = 0;
  let shouldDouble = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = Number(digits[i]);
    if (Number.isNaN(d)) return false;
    if (shouldDouble) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    sum += d;
    shouldDouble = !shouldDouble;
  }
  return sum % 10 === 0;
}

/**
 * Returns true when two PINs match (used in the change-PIN form).
 */
export function pinsMatch(a: string, b: string): boolean {
  return a === b && isValidPin(a);
}

/**
 * Reject sequential PINs like `1234` or repeated PINs like `1111` for new PIN
 * safety. (Soft check — server enforces the hard rule.)
 */
export function isWeakPin(pin: string): boolean {
  if (!isValidPin(pin)) return true;
  const seq = '0123456789';
  if (seq.includes(pin) || seq.split('').reverse().join('').includes(pin)) return true;
  return /^(\d)\1{3}$/.test(pin);
}
