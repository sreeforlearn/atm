package com.atm.card.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single ATM/Debit/Credit card.
 *
 * <p>The {@code cards} table is shared across services but only card-service
 * performs CRUD operations on card metadata.</p>
 */
@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_number", columnList = "card_number", unique = true),
        @Index(name = "idx_card_holder", columnList = "holder_name"),
        @Index(name = "idx_card_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", unique = true, nullable = false, updatable = false, length = 32)
    private String cardNumber;

    @Column(name = "holder_name", nullable = false, length = 128)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 16)
    private CardType cardType;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "daily_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "blocked_reason", length = 256)
    private String blockedReason;

    /**
     * Set the issued timestamp on creation if absent.
     */
    @PrePersist
    void onCreate() {
        this.issuedAt = this.issuedAt != null ? this.issuedAt : LocalDateTime.now();
    }

    /** Lifecycle status of a card. */
    public enum Status {
        ACTIVE, BLOCKED, EXPIRED, LOST
    }

    /** Type of card. */
    public enum CardType {
        DEBIT, CREDIT
    }
}
