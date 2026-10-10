package com.atm.account.repository;

import com.atm.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Account} entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find an account by its account number.
     *
     * @param accountNumber account number
     * @return optional account
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find all accounts linked to a card.
     *
     * @param cardNumber card number
     * @return list of accounts
     */
    List<Account> findByCardNumber(String cardNumber);

    /**
     * Check existence by account number.
     *
     * @param accountNumber account number to test
     * @return {@code true} if it exists
     */
    boolean existsByAccountNumber(String accountNumber);
}
