package com.atm.card.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for issuing a new card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCardRequest {

    @NotBlank(message = "Holder name is required")
    @Size(max = 128, message = "Holder name cannot exceed 128 characters")
    private String holderName;

    @NotNull(message = "Card type is required")
    private CardType cardType;

    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "0.00", message = "Daily limit cannot be negative")
    private BigDecimal dailyLimit;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.00", message = "Monthly limit cannot be negative")
    private BigDecimal monthlyLimit;

    /** Card type selection. */
    public enum CardType {
        DEBIT, CREDIT
    }
}
