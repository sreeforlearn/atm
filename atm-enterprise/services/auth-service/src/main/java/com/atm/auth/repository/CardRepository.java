package com.atm.auth.repository;

import com.atm.auth.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for {@link Card} entities.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Find a card by its 16-digit card number.
     *
     * @param cardNumber the 16-digit card number
     * @return optional card
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Check existence by card number.
     *
     * @param cardNumber card number to test
     * @return {@code true} if a card already exists for the given number
     */
    boolean existsByCardNumber(String cardNumber);
}
