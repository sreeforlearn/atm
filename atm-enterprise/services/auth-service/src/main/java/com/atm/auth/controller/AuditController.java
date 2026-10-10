package com.atm.auth.controller;

import com.atm.auth.dto.ApiResponse;
import com.atm.auth.entity.AuditLog;
import com.atm.auth.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit trail endpoints for card authentication events.
 */
@Tag(name = "Audit", description = "Card authentication audit trail")
@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Fetch paginated audit log entries for a card.
     *
     * @param cardNumber card number to filter on
     * @param page        page number (0-indexed)
     * @param size        page size
     * @return page of audit entries
     */
    @Operation(
            summary = "Get card audit trail",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAudit(
            @PathVariable @Parameter(description = "16-digit card number") String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> result = auditLogRepository
                .findByCardNumberOrderByTimestampDesc(cardNumber, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result, "Audit trail"));
    }
}
