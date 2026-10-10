package com.atm.card.entity;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks spending per card against daily and monthly limits.
 *
 * <p>Reset nightly / monthly by the {@link com.atm.card.service.CardService}
 * scheduled jobs.</p>
 */
@Entity
@Table(name = "card_limits", indexes = {
        @Index(name = "idx_card_limit_card", columnList = "card_number", unique = true),
        @Index(name = "idx_card_limit_reset", columnList = "last_reset_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", unique = true, nullable = false, length = 32)
    private String cardNumber;

    @Column(name = "daily_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailySpent;

    @Column(name = "monthly_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlySpent;

    @Column(name = "last_reset_date", nullable = false)
    private LocalDate lastResetDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Initialize created timestamp on insert.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = this.createdAt != null ? this.createdAt : now;
        this.updatedAt = now;
        if (this.dailySpent == null) {
            this.dailySpent = BigDecimal.ZERO;
        }
        if (this.monthlySpent == null) {
            this.monthlySpent = BigDecimal.ZERO;
        }
        if (this.lastResetDate == null) {
            this.lastResetDate = now.toLocalDate();
        }
    }
}
