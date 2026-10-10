package com.atm.account.service;

import com.atm.account.dto.AccountResponse;
import com.atm.account.dto.BalanceResponse;
import com.atm.account.dto.CreateAccountRequest;
import com.atm.account.entity.Account;
import com.atm.account.entity.AccountAudit;
import com.atm.account.exception.AccountNotFoundException;
import com.atm.account.exception.InsufficientFundsException;
import com.atm.account.repository.AccountAuditRepository;
import com.atm.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Account business logic - balance queries, creation, mutations and lifecycle.
 *
 * <p>All balance mutations are audited via {@link AccountAudit} entries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountAuditRepository accountAuditRepository;

    /**
     * Fetch an account by card number.
     *
     * @param cardNumber card number
     * @return list of account snapshots (one card may map to several accounts)
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCardNumber(String cardNumber) {
        return accountRepository.findByCardNumber(cardNumber).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetch an account by its account number.
     *
     * @param accountNumber account number
     * @return account snapshot
     * @throws AccountNotFoundException if no such account exists
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        return toResponse(findAccountOrThrow(accountNumber));
    }

    /**
     * Fetch the balance snapshot for an account.
     *
     * @param accountNumber account number
     * @return balance response
     * @throws AccountNotFoundException if no such account exists
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        BigDecimal available = account.getStatus() == Account.AccountStatus.FROZEN
                ? BigDecimal.ZERO
                : account.getBalance();
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .availableBalance(available)
                .currency(account.getCurrency())
                .lastUpdated(account.getUpdatedAt())
                .build();
    }

    /**
     * Create a new account.
     *
     * @param request creation payload
     * @return account snapshot
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account.AccountType type;
        try {
            type = Account.AccountType.valueOf(request.getAccountType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid account type: " + request.getAccountType());
        }

        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .cardNumber(request.getCardNumber())
                .holderName(request.getHolderName())
                .balance(request.getInitialDeposit())
                .accountType(type)
                .status(Account.AccountStatus.ACTIVE)
                .currency("INR")
                .build();
        Account saved = accountRepository.save(account);

        // Record initial deposit as a CREDIT audit entry
        recordAudit(saved.getAccountNumber(), "CREDIT", saved.getBalance(),
                saved.getBalance(), "INITIAL_DEPOSIT", "SYSTEM");

        return toResponse(saved);
    }

    /**
     * Adjust an account's balance (credit or debit) atomically.
     *
     * @param accountNumber account number
     * @param amount         amount (must be positive)
     * @param type           {@code "CREDIT"} or {@code "DEBIT"}
     * @return updated balance
     * @throws AccountNotFoundException      if no such account
     * @throws InsufficientFundsException    if debit would push balance below zero
     * @throws IllegalStateException         if account is frozen
     */
    @Transactional
    public BigDecimal updateBalance(String accountNumber, BigDecimal amount, String type) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        Account account = findAccountOrThrow(accountNumber);
        if (account.getStatus() == Account.AccountStatus.FROZEN) {
            throw new IllegalStateException("Account is frozen: " + accountNumber);
        }

        BigDecimal newBalance;
        String action;
        switch (type.toUpperCase()) {
            case "CREDIT":
                newBalance = account.getBalance().add(amount);
                action = "CREDIT";
                break;
            case "DEBIT":
                if (account.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientFundsException(
                            "Insufficient funds: available=" + account.getBalance()
                                    + ", requested=" + amount);
                }
                newBalance = account.getBalance().subtract(amount);
                action = "DEBIT";
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid balance update type: " + type);
        }

        account.setBalance(newBalance);
        accountRepository.save(account);
        recordAudit(accountNumber, action, amount, newBalance, UUID.randomUUID().toString(), "TRANSACTION-SERVICE");

        return newBalance;
    }

    /**
     * Freeze an account (no transactions permitted).
     *
     * @param accountNumber account number
     * @throws AccountNotFoundException if no such account
     */
    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
        recordAudit(accountNumber, "FREEZE", BigDecimal.ZERO, account.getBalance(),
                UUID.randomUUID().toString(), "ADMIN");
    }

    /**
     * Internal helper to fetch the entity or throw.
     */
    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountNumber));
    }

    /**
     * Map the entity to its REST response form.
     */
    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .accountType(account.getAccountType().name())
                .status(account.getStatus().name())
                .currency(account.getCurrency())
                .build();
    }

    /**
     * Persist an audit record for a balance mutation.
     */
    private void recordAudit(String accountNumber, String action, BigDecimal amount,
                             BigDecimal balanceAfter, String reference, String performedBy) {
        AccountAudit audit = AccountAudit.builder()
                .accountNumber(accountNumber)
                .action(action)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceNumber(reference)
                .performedBy(performedBy)
                .timestamp(LocalDateTime.now())
                .build();
        accountAuditRepository.save(audit);
    }

    /**
     * Generate a unique 14-digit account number.
     */
    private String generateAccountNumber() {
        String num;
        do {
            // 14-digit number with leading 2 prefix for ATM Enterprise accounts
            num = "20" + String.format("%012d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(num));
        return num;
    }
}
