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
 * Request payload for updating card spending limits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLimitRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 8, max = 32, message = "Card number must be between 8 and 32 characters")
    private String cardNumber;

    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "0.00", message = "Daily limit cannot be negative")
    private BigDecimal dailyLimit;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.00", message = "Monthly limit cannot be negative")
    private BigDecimal monthlyLimit;
}
