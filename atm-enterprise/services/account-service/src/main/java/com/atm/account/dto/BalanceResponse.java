package com.atm.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance snapshot returned by GET /api/accounts/balance/{accountNumber}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private LocalDateTime lastUpdated;
}
