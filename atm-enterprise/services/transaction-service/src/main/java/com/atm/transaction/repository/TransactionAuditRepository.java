package com.atm.transaction.repository;

import com.atm.transaction.entity.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link TransactionAudit} entries.
 */
@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {

    /**
     * Return all audit entries for a given transaction id ordered naturally by id.
     *
     * @param transactionId the transaction id
     * @return list of audit entries
     */
    List<TransactionAudit> findByTransactionIdOrderByIdAsc(Long transactionId);
}
