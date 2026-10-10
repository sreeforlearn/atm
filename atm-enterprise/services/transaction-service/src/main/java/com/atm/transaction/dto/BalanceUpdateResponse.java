package com.atm.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response shape returned by account-service debit/credit endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdateResponse {

    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private String status;
}
