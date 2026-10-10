package com.atm.card.repository;

import com.atm.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Card} entities.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Look up a card by its unique card number.
     *
     * @param cardNumber the card number
     * @return optional card
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Look up all cards belonging to a particular holder.
     *
     * @param holderName the holder name (exact match)
     * @return list of cards
     */
    List<Card> findByHolderName(String holderName);

    /**
     * Check existence of a card by its card number.
     *
     * @param cardNumber the card number
     * @return true if exists
     */
    boolean existsByCardNumber(String cardNumber);
}
