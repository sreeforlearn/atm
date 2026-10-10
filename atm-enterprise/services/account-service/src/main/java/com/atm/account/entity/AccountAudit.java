package com.atm.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit trail entry capturing every balance mutation against an account.
 */
@Entity
@Table(
        name = "account_audits",
        indexes = {
                @Index(name = "idx_acc_audit_account", columnList = "account_number"),
                @Index(name = "idx_acc_audit_timestamp", columnList = "timestamp")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /** {@code CREDIT} increases the balance, {@code DEBIT} decreases it. */
    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_number", length = 60)
    private String referenceNumber;

    @Column(name = "performed_by", length = 60)
    private String performedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
