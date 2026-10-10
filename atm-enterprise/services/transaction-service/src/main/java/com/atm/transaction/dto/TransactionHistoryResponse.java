package com.atm.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated container for transaction history responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {

    private List<TransactionResponse> transactions;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
