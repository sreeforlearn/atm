package com.atm.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * Audit trail entry capturing contextual metadata for each transaction action
 * (who, what, when, from where).
 */
@Entity
@Table(name = "transaction_audits", indexes = {
        @Index(name = "idx_audit_txn", columnList = "transaction_id"),
        @Index(name = "idx_audit_performed_by", columnList = "performed_by")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "performed_by", length = 64)
    private String performedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * PrePersist hook ensuring timestamp is populated.
     */
    @PrePersist
    void onCreate() {
        this.timestamp = this.timestamp != null ? this.timestamp : LocalDateTime.now();
    }
}
