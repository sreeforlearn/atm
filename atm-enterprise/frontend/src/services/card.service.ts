import api from './api';
import type { ApiResponse, Card } from '@/types';

/**
 * Card service — wraps `/api/v1/cards/*`.
 */
export const cardService = {
  /**
   * Fetch details of the authenticated user's card.
   */
  async getCardDetails(): Promise<Card> {
    const { data } = await api.get<ApiResponse<Card>>('/cards/me');
    return data.data;
  },

  /**
   * Verify a card + PIN without issuing a session token (read-only check).
   */
  async verifyCard(cardNumber: string, pin: string): Promise<boolean> {
    const { data } = await api.post<ApiResponse<boolean>>('/cards/verify', {
      cardNumber,
      pin,
    });
    return data.data;
  },

  /**
   * Block a card — used from the "lost my card" self-service flow.
   */
  async blockCard(cardId: string): Promise<void> {
    await api.patch<ApiResponse<null>>(`/cards/${cardId}/block`);
  },
};
