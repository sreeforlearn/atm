package com.atm.transaction.controller;

import com.atm.transaction.dto.ApiResponse;
import com.atm.transaction.dto.DepositRequest;
import com.atm.transaction.dto.TransactionHistoryResponse;
import com.atm.transaction.dto.TransactionResponse;
import com.atm.transaction.dto.TransferRequest;
import com.atm.transaction.dto.WithdrawRequest;
import com.atm.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the ATM Enterprise transaction endpoints.
 *
 * <p>Base path: {@code /api/transactions}</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Withdrawals, deposits, transfers and history lookup")
public class TransactionController {

    private static final String CARD_HEADER = "X-Card-Number";

    private final TransactionService transactionService;

    /**
     * Initiate an ATM cash withdrawal.
     *
     * @param request  the withdrawal request
     * @param cardNumber the card number initiating the withdrawal (header)
     * @param httpRequest servlet request to extract caller IP
     * @return the created transaction response
     */
    @Operation(summary = "Withdraw cash from an account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Withdrawal processed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid request payload")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = CARD_HEADER, required = false) String cardNumber,
            HttpServletRequest httpRequest) {
        log.info("Withdrawal request from account {}", request.getAccountNumber());
        TransactionResponse response = transactionService.withdraw(request, cardNumber, extractIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response, "Withdrawal processed"));
    }

    /**
     * Initiate an ATM cash deposit.
     *
     * @param request  the deposit request
     * @param cardNumber the card number initiating the deposit (header)
     * @param httpRequest servlet request to extract caller IP
     * @return the created transaction response
     */
    @Operation(summary = "Deposit cash to an account")
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = CARD_HEADER, required = false) String cardNumber,
            HttpServletRequest httpRequest) {
        log.info("Deposit request to account {}", request.getAccountNumber());
        TransactionResponse response = transactionService.deposit(request, cardNumber, extractIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response, "Deposit processed"));
    }

    /**
     * Initiate an inter-account transfer.
     *
     * @param request  the transfer request
     * @param cardNumber the card number initiating the transfer (header)
     * @param httpRequest servlet request to extract caller IP
     * @return the created transaction response
     */
    @Operation(summary = "Transfer funds between accounts")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = CARD_HEADER, required = false) String cardNumber,
            HttpServletRequest httpRequest) {
        log.info("Transfer request from {} to {}", request.getFromAccount(), request.getToAccount());
        TransactionResponse response = transactionService.transfer(request, cardNumber, extractIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response, "Transfer processed"));
    }

    /**
     * Look up a transaction by its reference number.
     *
     * @param referenceNumber the UUID reference
     * @return the transaction response
     */
    @Operation(summary = "Fetch a transaction by reference number")
    @GetMapping("/{referenceNumber}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @Parameter(description = "UUID reference of the transaction", required = true)
            @PathVariable String referenceNumber) {
        TransactionResponse response = transactionService.getTransactionByReference(referenceNumber);
        return ResponseEntity.ok(ApiResponse.ok(response, "Transaction retrieved"));
    }

    /**
     * Fetch paginated transaction history for an account.
     *
     * @param accountNumber the account number
     * @param page          zero-based page index (default 0)
     * @param size          page size (default 10)
     * @return the history response
     */
    @Operation(summary = "Paginated transaction history for an account")
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<TransactionHistoryResponse>> getHistory(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        TransactionHistoryResponse response = transactionService.getTransactionHistory(accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response, "Transaction history retrieved"));
    }

    /**
     * Extract the originating IP from the servlet request, preferring
     * X-Forwarded-For when present.
     *
     * @param request the servlet request
     * @return the IP address
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
