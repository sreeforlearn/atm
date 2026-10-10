package com.atm.transaction.dto;

import com.atm.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a single transaction record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String referenceNumber;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private BigDecimal balanceAfter;
    private LocalDateTime timestamp;
    private String remark;

    /**
     * Map a {@link Transaction} entity to its response representation.
     *
     * @param transaction the persisted entity
     * @return the response DTO
     */
    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .referenceNumber(transaction.getReferenceNumber())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .balanceAfter(transaction.getBalanceAfter())
                .timestamp(transaction.getTimestamp())
                .remark(transaction.getRemark())
                .build();
    }
}
