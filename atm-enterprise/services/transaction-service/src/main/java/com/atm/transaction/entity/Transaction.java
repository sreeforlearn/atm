package com.atm.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single financial transaction in the ATM Enterprise system.
 *
 * <p>Each transaction carries its own UUID reference number, source/target
 * accounts, monetary amount, type (WITHDRAW, DEPOSIT, TRANSFER, PIN_CHANGE)
 * and execution status. Pre/post balance snapshots are stored for audit.</p>
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_reference", columnList = "reference_number", unique = true),
        @Index(name = "idx_txn_from_account", columnList = "from_account"),
        @Index(name = "idx_txn_to_account", columnList = "to_account"),
        @Index(name = "idx_txn_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", unique = true, nullable = false, updatable = false, length = 36)
    private String referenceNumber;

    @Column(name = "from_account", nullable = false, length = 32)
    private String fromAccount;

    @Column(name = "to_account", length = 32)
    private String toAccount;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "balance_before", precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "remark", length = 256)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private Channel channel;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Sets the reference number (if absent) and timestamp before persistence.
     */
    @PrePersist
    void onCreate() {
        if (this.referenceNumber == null || this.referenceNumber.isBlank()) {
            this.referenceNumber = UUID.randomUUID().toString();
        }
        this.timestamp = LocalDateTime.now();
    }

    /** Transaction type classification. */
    public enum TransactionType {
        WITHDRAW, DEPOSIT, TRANSFER, PIN_CHANGE
    }

    /** Lifecycle status of a transaction. */
    public enum TransactionStatus {
        SUCCESS, FAILED, PENDING
    }

    /** Channel through which the transaction was initiated. */
    public enum Channel {
        ATM, WEB, MOBILE
    }
}
