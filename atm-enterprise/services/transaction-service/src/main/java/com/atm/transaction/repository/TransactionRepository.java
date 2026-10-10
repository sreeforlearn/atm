package com.atm.transaction.repository;

import com.atm.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Transaction} entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Look up a transaction by its unique reference number.
     *
     * @param referenceNumber the UUID reference number
     * @return optional transaction
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Fetch transactions where the given account is either the source or the destination,
     * ordered by timestamp descending.
     *
     * @param fromAccount source account number
     * @param toAccount   destination account number
     * @param pageable     paging information
     * @return page of matching transactions
     */
    Page<Transaction> findByFromAccountOrToAccountOrderByTimestampDesc(
            String fromAccount, String toAccount, Pageable pageable);

    /**
     * Fetch all outgoing transactions for a given account, newest first.
     *
     * @param fromAccount source account number
     * @param pageable    paging information
     * @return page of outgoing transactions
     */
    Page<Transaction> findByFromAccountOrderByTimestampDesc(String fromAccount, Pageable pageable);

    /**
     * Fetch all transactions within a date range (inclusive).
     *
     * @param start    start timestamp
     * @param end      end timestamp
     * @param pageable paging information
     * @return page of transactions in the range
     */
    Page<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Count transactions matching a status that occurred after the given timestamp.
     *
     * @param status     status filter
     * @param timestamp  lower bound (exclusive)
     * @return count of matching transactions
     */
    long countByStatusAndTimestampAfter(Transaction.TransactionStatus status, LocalDateTime timestamp);
}
