package com.atm.card.dto;

import com.atm.card.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response payload representing card details.
 *
 * <p>The card number is masked for security reasons; the expiry date is
 * formatted as {@code MM/yy}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    private String cardNumber;     // masked
    private String holderName;
    private Card.Status status;
    private Card.CardType cardType;
    private String expiryDate;     // MM/yy
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlySpent;

    /**
     * Mask a card number keeping the last 4 digits visible.
     *
     * @param cardNumber the raw card number
     * @return masked form
     */
    public static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 5) {
            return "****";
        }
        int length = cardNumber.length();
        return "*".repeat(length - 4) + cardNumber.substring(length - 4);
    }
}
