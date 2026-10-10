package com.atm.card.repository;

import com.atm.card.entity.CardLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CardLimit} entities (spending trackers).
 */
@Repository
public interface CardLimitRepository extends JpaRepository<CardLimit, Long> {

    /**
     * Fetch the spending tracker for the given card.
     *
     * @param cardNumber card number
     * @return optional CardLimit
     */
    Optional<CardLimit> findByCardNumber(String cardNumber);

    /**
     * Return all limit rows whose last reset date is before the given day
     * (used by the daily reset job).
     *
     * @param today today's date
     * @return list of stale limit rows
     */
    List<CardLimit> findByLastResetDateBefore(LocalDate today);

    /**
     * Bulk-reset daily spent to zero for all rows (used by daily scheduler).
     *
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE CardLimit cl SET cl.dailySpent = 0, cl.lastResetDate = :today, cl.updatedAt = CURRENT_TIMESTAMP")
    int resetAllDailySpent(@Param("today") LocalDate today);

    /**
     * Bulk-reset monthly spent to zero for all rows (used by monthly scheduler).
     *
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE CardLimit cl SET cl.monthlySpent = 0, cl.updatedAt = CURRENT_TIMESTAMP")
    int resetAllMonthlySpent();
}
