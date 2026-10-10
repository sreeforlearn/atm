package com.atm.auth.repository;

import com.atm.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link AuditLog} entities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Fetch the audit trail for a card, newest first, paged.
     *
     * @param cardNumber card number filter
     * @param pageable   paging info
     * @return page of audit log entries
     */
    Page<AuditLog> findByCardNumberOrderByTimestampDesc(String cardNumber, Pageable pageable);
}
