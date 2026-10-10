package com.atm.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight response shape returned by account-service endpoints.
 *
 * <p>This is a local DTO mirroring the response of
 * {@code account-service} so we can deserialize responses without pulling
 * that service as a dependency.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private String accountNumber;
    private String holderName;
    private BigDecimal balance;
    private String currency;
    private String status;
}
