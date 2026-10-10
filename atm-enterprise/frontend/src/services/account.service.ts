import api from './api';
import type { Account, ApiResponse, BalanceResponse } from '@/types';

/**
 * Account service — wraps `/api/v1/accounts/*`.
 */
export const accountService = {
  /**
   * List all accounts owned by the authenticated customer.
   */
  async getAccounts(): Promise<Account[]> {
    const { data } = await api.get<ApiResponse<Account[]>>('/accounts');
    return data.data;
  },

  /**
   * Fetch the account that is linked to the authenticated card.
   */
  async getAccountByCard(): Promise<Account> {
    const { data } = await api.get<ApiResponse<Account>>('/accounts/card');
    return data.data;
  },

  /**
   * Get the latest balance snapshot for an account.
   */
  async getBalance(accountId: string): Promise<BalanceResponse> {
    const { data } = await api.get<ApiResponse<BalanceResponse>>(
      `/accounts/${accountId}/balance`,
    );
    return data.data;
  },
};
