package com.atm.account.controller;

import com.atm.account.dto.AccountResponse;
import com.atm.account.dto.ApiResponse;
import com.atm.account.dto.BalanceResponse;
import com.atm.account.dto.CreateAccountRequest;
import com.atm.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for account lookup, creation and freezing.
 */
@Tag(name = "Accounts", description = "Bank account & balance management")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Get a specific account by its account number.
     */
    @Operation(summary = "Get account by number", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable @Parameter(description = "Account number") String accountNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountService.getAccount(accountNumber), "Account found"));
    }

    /**
     * Get the balance of an account.
     */
    @Operation(summary = "Get balance", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountService.getBalance(accountNumber), "Balance retrieved"));
    }

    /**
     * Get all accounts linked to a card.
     */
    @Operation(summary = "Get accounts by card number", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/card/{cardNumber}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByCard(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountService.getAccountsByCardNumber(cardNumber), "Accounts found"));
    }

    /**
     * Create a new account. The card number may also be sourced from the
     * authenticated principal if not provided in the body.
     */
    @Operation(summary = "Create account", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {
        // Fall back to authenticated card number if request doesn't carry it
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            request.setCardNumber(authentication.getName());
        }
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Account created"));
    }

    /**
     * Freeze an account (block all transactions).
     */
    @Operation(summary = "Freeze account", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/freeze/{accountNumber}")
    public ResponseEntity<ApiResponse<Void>> freezeAccount(
            @PathVariable String accountNumber) {
        accountService.freezeAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Account frozen"));
    }
}
