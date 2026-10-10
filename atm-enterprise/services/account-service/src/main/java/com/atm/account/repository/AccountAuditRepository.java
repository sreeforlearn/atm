package com.atm.account.repository;

import com.atm.account.entity.AccountAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link AccountAudit} entities.
 */
@Repository
public interface AccountAuditRepository extends JpaRepository<AccountAudit, Long> {

    /**
     * Fetch the audit trail for an account, newest first, paged.
     *
     * @param accountNumber account number
     * @param pageable      paging info
     * @return page of audit entries
     */
    Page<AccountAudit> findByAccountNumberOrderByTimestampDesc(String accountNumber, Pageable pageable);
}
