/**
 * Shared TypeScript domain types for the ATM Enterprise frontend.
 *
 * All API responses are wrapped in {@link ApiResponse} so that the gateway's
 * `{ success, message, data }` envelope is consistently typed.
 */

/** Generic API envelope returned by the API Gateway. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  /** Present on paginated endpoints. */
  timestamp?: string;
  /** HTTP status code echoed by the gateway. */
  statusCode?: number;
}

/** Paginated response shape. */
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ----------------------------- User & Auth -----------------------------

export interface User {
  id: string;
  customerId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
}

export interface LoginRequest {
  cardNumber: string;
  pin: string;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  user: User;
  card?: Card;
}

export interface PinChangeRequest {
  cardId: string;
  oldPin: string;
  newPin: string;
}

// ----------------------------- Accounts & Cards -----------------------------

export type AccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export interface Account {
  id: string;
  accountNumber: string;
  accountType: AccountType;
  bankName: string;
  branchName?: string;
  ifscCode?: string;
  balance: number;
  availableBalance?: number;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
}

export type CardType = 'DEBIT' | 'CREDIT' | 'ATM_ONLY';
export type CardStatus = 'ACTIVE' | 'BLOCKED' | 'EXPIRED' | 'LOST';

export interface Card {
  id: string;
  cardNumber: string;
  cardHolderName: string;
  expiryMonth: number;
  expiryYear: number;
  type: CardType;
  status: CardStatus;
  accountId?: string;
  dailyLimit?: number;
}

// ----------------------------- Transactions -----------------------------

export enum TransactionType {
  WITHDRAW = 'WITHDRAW',
  DEPOSIT = 'DEPOSIT',
  TRANSFER = 'TRANSFER',
  REFUND = 'REFUND',
}

export interface Transaction {
  id: string;
  referenceNumber: string;
  accountId: string;
  type: TransactionType;
  amount: number;
  /** Direction relative to the account: CR = credit, DR = debit. */
  direction: 'CR' | 'DR';
  description?: string;
  remark?: string;
  balanceAfter?: number;
  counterpartyAccount?: string;
  counterpartyName?: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  createdAt: string;
}

export interface BalanceResponse {
  accountId: string;
  accountNumber: string;
  balance: number;
  availableBalance: number;
  currency: string;
  lastUpdated: string;
}

// ----------------------------- Transaction Requests -----------------------------

export interface WithdrawRequest {
  accountId: string;
  amount: number;
  pin: string;
}

export interface DepositRequest {
  accountId: string;
  amount: number;
  source: 'CASH' | 'CHEQUE';
  remark?: string;
}

export interface TransferRequest {
  fromAccountId: string;
  toAccountNumber: string;
  amount: number;
  remark?: string;
  pin: string;
}

// ----------------------------- Misc -----------------------------

export interface Option<T> {
  label: string;
  value: T;
}
