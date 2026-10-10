package com.atm.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for ATM cash deposits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 8, max = 32, message = "Account number must be between 8 and 32 characters")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Deposit amount must be positive")
    @Max(value = 1000000, message = "Maximum deposit amount is 1000000 per transaction")
    private BigDecimal amount;

    @NotNull(message = "Deposit source is required")
    private Source source;

    @Size(max = 256, message = "Remark cannot exceed 256 characters")
    private String remark;

    /** Source of funds for the deposit. */
    public enum Source {
        CASH, CHEQUE
    }
}
