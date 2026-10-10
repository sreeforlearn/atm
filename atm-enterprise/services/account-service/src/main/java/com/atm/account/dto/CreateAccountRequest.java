package com.atm.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for creating a new account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 120)
    private String holderName;

    @NotBlank(message = "Account type is required")
    private String accountType;  // SAVINGS / CURRENT

    @NotNull(message = "Initial deposit is required")
    @Min(value = 0, message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 19)
    private String cardNumber;
}
