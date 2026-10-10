package com.atm.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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
 * Request payload for ATM cash withdrawals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 8, max = 32, message = "Account number must be between 8 and 32 characters")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum withdrawal amount is 100")
    @Max(value = 50000, message = "Maximum withdrawal amount is 50000 per transaction")
    private BigDecimal amount;

    @Size(max = 256, message = "Remark cannot exceed 256 characters")
    private String remark;
}
