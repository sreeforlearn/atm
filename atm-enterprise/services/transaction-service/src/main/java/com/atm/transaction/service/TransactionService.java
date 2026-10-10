package com.atm.transaction.service;

import com.atm.transaction.dto.AccountDto;
import com.atm.transaction.dto.BalanceUpdateResponse;
import com.atm.transaction.dto.DepositRequest;
import com.atm.transaction.dto.TransactionHistoryResponse;
import com.atm.transaction.dto.TransactionResponse;
import com.atm.transaction.dto.TransferRequest;
import com.atm.transaction.dto.WithdrawRequest;
import com.atm.transaction.entity.Transaction;
import com.atm.transaction.entity.TransactionAudit;
import com.atm.transaction.exception.AccountServiceException;
import com.atm.transaction.exception.InsufficientFundsException;
import com.atm.transaction.exception.TransactionNotFoundException;
import com.atm.transaction.kafka.TransactionEvent;
import com.atm.transaction.kafka.TransactionEventProducer;
import com.atm.transaction.repository.TransactionAuditRepository;
import com.atm.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business service orchestrating transaction lifecycle.
 *
 * <p>Each transaction method:</p>
 * <ol>
 *   <li>Pre-checks balance via account-service</li>
 *   <li>Persists the transaction entity with appropriate status</li>
 *   <li>Publishes a {@link TransactionEvent} to the transaction-events Kafka topic</li>
 *   <li>Records an audit entry capturing the caller context</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionAuditRepository auditRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventProducer eventProducer;

    /**
     * Process an ATM cash withdrawal.
     *
     * @param request     the withdrawal request
     * @param cardNumber  the card number initiating the request (may be {@code null})
     * @param ipAddress   the originating IP address
     * @return the created transaction response
     * @throws InsufficientFundsException if account balance is less than amount
     * @throws AccountServiceException    if the account-service call fails
     */
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, String cardNumber, String ipAddress) {
        log.info("Processing withdrawal of {} from account {}",
                request.getAmount(), request.getAccountNumber());

        AccountDto account = accountServiceClient.getAccount(request.getAccountNumber());
        if (account.getBalance() == null || account.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient funds for account {}: balance={}, requested={}",
                    request.getAccountNumber(), account.getBalance(), request.getAmount());
            Transaction failed = persistFailedTransaction(
                    request.getAccountNumber(),
                    null,
                    request.getAmount(),
                    Transaction.TransactionType.WITHDRAW,
                    account.getBalance(),
                    request.getRemark(),
                    "Insufficient funds");
            audit(failed.getId(), "WITHDRAW_FAILED", cardNumber, ipAddress);
            throw new InsufficientFundsException(
                    "Insufficient funds: available=" + account.getBalance()
                            + ", requested=" + request.getAmount());
        }

        BalanceUpdateResponse debitResult = accountServiceClient.debit(
                request.getAccountNumber(), request.getAmount(), UUID.randomUUID().toString());
        Transaction transaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .fromAccount(request.getAccountNumber())
                .toAccount(null)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.WITHDRAW)
                .status(Transaction.TransactionStatus.SUCCESS)
                .balanceBefore(debitResult.getPreviousBalance())
                .balanceAfter(debitResult.getNewBalance())
                .remark(request.getRemark())
                .channel(Transaction.Channel.ATM)
                .build();
        Transaction saved = transactionRepository.save(transaction);
        eventProducer.send(buildEvent(saved));
        audit(saved.getId(), "WITHDRAW", cardNumber, ipAddress);
        log.info("Withdrawal completed: ref={}, newBalance={}",
                saved.getReferenceNumber(), saved.getBalanceAfter());
        return TransactionResponse.from(saved);
    }

    /**
     * Process an ATM cash deposit.
     *
     * @param request    the deposit request
     * @param cardNumber the card number initiating the request (may be {@code null})
     * @param ipAddress  the originating IP address
     * @return the created transaction response
     */
    @Transactional
    public TransactionResponse deposit(DepositRequest request, String cardNumber, String ipAddress) {
        log.info("Processing deposit of {} to account {}",
                request.getAmount(), request.getAccountNumber());

        AccountDto account = accountServiceClient.getAccount(request.getAccountNumber());
        BalanceUpdateResponse creditResult = accountServiceClient.credit(
                request.getAccountNumber(), request.getAmount(), UUID.randomUUID().toString());

        Transaction transaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .fromAccount(request.getAccountNumber())
                .toAccount(request.getAccountNumber())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.SUCCESS)
                .balanceBefore(creditResult.getPreviousBalance())
                .balanceAfter(creditResult.getNewBalance())
                .remark(request.getRemark() != null ? request.getRemark() : "Deposit: " + request.getSource())
                .channel(Transaction.Channel.ATM)
                .build();
        Transaction saved = transactionRepository.save(transaction);
        eventProducer.send(buildEvent(saved));
        audit(saved.getId(), "DEPOSIT", cardNumber, ipAddress);
        log.info("Deposit completed: ref={}, newBalance={}",
                saved.getReferenceNumber(), saved.getBalanceAfter());
        return TransactionResponse.from(saved);
    }

    /**
     * Process an inter-account transfer.
     *
     * @param request    the transfer request
     * @param cardNumber the card number initiating the request (may be {@code null})
     * @param ipAddress  the originating IP address
     * @return the created transaction response
     * @throws InsufficientFundsException if source account has insufficient funds
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String cardNumber, String ipAddress) {
        log.info("Processing transfer of {} from {} to {}",
                request.getAmount(), request.getFromAccount(), request.getToAccount());

        if (request.getFromAccount().equalsIgnoreCase(request.getToAccount())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        AccountDto source = accountServiceClient.getAccount(request.getFromAccount());
        if (source.getBalance() == null || source.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient funds for transfer: balance={}, requested={}",
                    source.getBalance(), request.getAmount());
            Transaction failed = persistFailedTransaction(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    Transaction.TransactionType.TRANSFER,
                    source.getBalance(),
                    request.getRemark(),
                    "Insufficient funds");
            audit(failed.getId(), "TRANSFER_FAILED", cardNumber, ipAddress);
            throw new InsufficientFundsException(
                    "Insufficient funds: available=" + source.getBalance()
                            + ", requested=" + request.getAmount());
        }

        // Debit source then credit destination
        BalanceUpdateResponse debitResult = accountServiceClient.debit(
                request.getFromAccount(), request.getAmount(), UUID.randomUUID().toString());
        BalanceUpdateResponse creditResult;
        try {
            creditResult = accountServiceClient.credit(
                    request.getToAccount(), request.getAmount(), UUID.randomUUID().toString());
        } catch (AccountServiceException ex) {
            log.error("Credit leg failed, attempting compensating credit on source. Ref={}",
                    debitResult.getAccountNumber());
            accountServiceClient.credit(request.getFromAccount(), request.getAmount(), UUID.randomUUID().toString());
            throw ex;
        }

        Transaction transaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.SUCCESS)
                .balanceBefore(debitResult.getPreviousBalance())
                .balanceAfter(debitResult.getNewBalance())
                .remark(request.getRemark())
                .channel(Transaction.Channel.WEB)
                .build();
        Transaction saved = transactionRepository.save(transaction);
        eventProducer.send(buildEvent(saved));
        audit(saved.getId(), "TRANSFER", cardNumber, ipAddress);
        log.info("Transfer completed: ref={}, sourceBalance={}, destBalance={}",
                saved.getReferenceNumber(), saved.getBalanceAfter(), creditResult.getNewBalance());
        return TransactionResponse.from(saved);
    }

    /**
     * Fetch a transaction by its unique reference number.
     *
     * @param referenceNumber the UUID reference
     * @return the transaction response
     * @throws TransactionNotFoundException if no such transaction exists
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found for reference: " + referenceNumber));
    }

    /**
     * Fetch paginated transaction history for an account (as source OR destination).
     *
     * @param accountNumber the account number
     * @param page          zero-based page index
     * @param size          page size
     * @return paginated history response
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> result = transactionRepository
                .findByFromAccountOrToAccountOrderByTimestampDesc(accountNumber, accountNumber, pageable);
        List<TransactionResponse> items = result.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        return TransactionHistoryResponse.builder()
                .transactions(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .build();
    }

    /* ---------- helpers ---------- */

    private Transaction persistFailedTransaction(String fromAccount,
                                                 String toAccount,
                                                 BigDecimal amount,
                                                 Transaction.TransactionType type,
                                                 BigDecimal balanceBefore,
                                                 String remark,
                                                 String failureReason) {
        Transaction failed = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .type(type)
                .status(Transaction.TransactionStatus.FAILED)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore)
                .remark(remark != null ? remark + " | " + failureReason : failureReason)
                .channel(Transaction.Channel.ATM)
                .build();
        return transactionRepository.save(failed);
    }

    private void audit(Long transactionId, String action, String performedBy, String ipAddress) {
        auditRepository.save(TransactionAudit.builder()
                .transactionId(transactionId)
                .action(action)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .build());
    }

    private TransactionEvent buildEvent(Transaction txn) {
        return TransactionEvent.builder()
                .referenceNumber(txn.getReferenceNumber())
                .type(txn.getType().name())
                .amount(txn.getAmount())
                .fromAccount(txn.getFromAccount())
                .toAccount(txn.getToAccount())
                .status(txn.getStatus().name())
                .timestamp(txn.getTimestamp())
                .build();
    }
}
