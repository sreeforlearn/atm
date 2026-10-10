package com.atm.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Account snapshot returned by GET /api/accounts/{accountNumber}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String accountNumber;
    private String holderName;
    private BigDecimal balance;
    private String accountType;
    private String status;
    private String currency;
}
