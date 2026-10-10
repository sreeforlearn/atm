import api from './api';
import type {
  ApiResponse,
  DepositRequest,
  PaginatedResponse,
  Transaction,
  TransferRequest,
  WithdrawRequest,
} from '@/types';

/**
 * Transaction service — wraps `/api/v1/transactions/*`.
 *
 * Mutations (`withdraw`, `deposit`, `transfer`) invalidate the
 * `transactions` and `balance` React Query keys (see hooks/useTransactions).
 */
export const transactionService = {
  /**
   * Withdraw cash from the given account.
   */
  async withdraw(payload: WithdrawRequest): Promise<Transaction> {
    const { data } = await api.post<ApiResponse<Transaction>>(
      '/transactions/withdraw',
      payload,
    );
    return data.data;
  },

  /**
   * Deposit cash or a cheque into the given account.
   */
  async deposit(payload: DepositRequest): Promise<Transaction> {
    const { data } = await api.post<ApiResponse<Transaction>>(
      '/transactions/deposit',
      payload,
    );
    return data.data;
  },

  /**
   * Transfer funds between two accounts.
   */
  async transfer(payload: TransferRequest): Promise<Transaction> {
    const { data } = await api.post<ApiResponse<Transaction>>(
      '/transactions/transfer',
      payload,
    );
    return data.data;
  },

  /**
   * Paginated transaction history for an account.
   */
  async getTransactions(
    accountId: string,
    page = 0,
    size = 10,
  filters?: { from?: string; to?: string; type?: string },
  ): Promise<PaginatedResponse<Transaction>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });
    if (filters?.from) params.append('from', filters.from);
    if (filters?.to) params.append('to', filters.to);
    if (filters?.type) params.append('type', filters.type);

    const { data } = await api.get<ApiResponse<PaginatedResponse<Transaction>>>(
      `/accounts/${accountId}/transactions?${params.toString()}`,
    );
    return data.data;
  },

  /**
   * Fetch a single transaction by id (used for receipt screen).
   */
  async getTransactionById(id: string): Promise<Transaction> {
    const { data } = await api.get<ApiResponse<Transaction>>(
      `/transactions/${id}`,
    );
    return data.data;
  },
};
