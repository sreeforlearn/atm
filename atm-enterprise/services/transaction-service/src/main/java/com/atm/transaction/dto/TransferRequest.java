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
 * Request payload for inter-account transfers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    @Size(min = 8, max = 32, message = "Source account number must be between 8 and 32 characters")
    private String fromAccount;

    @NotBlank(message = "Destination account number is required")
    @Size(min = 8, max = 32, message = "Destination account number must be between 8 and 32 characters")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Transfer amount must be positive")
    @Max(value = 500000, message = "Maximum transfer amount is 500000 per transaction")
    private BigDecimal amount;

    @Size(max = 256, message = "Remark cannot exceed 256 characters")
    private String remark;
}
