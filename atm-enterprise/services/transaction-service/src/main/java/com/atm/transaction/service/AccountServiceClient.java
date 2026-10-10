package com.atm.transaction.service;

import com.atm.transaction.dto.AccountDto;
import com.atm.transaction.dto.BalanceUpdateRequest;
import com.atm.transaction.dto.BalanceUpdateResponse;
import com.atm.transaction.exception.AccountServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Thin HTTP client wrapping the account-service REST endpoints.
 *
 * <p>Operations exposed:</p>
 * <ul>
 *   <li>{@code GET /api/accounts/{accountNumber}} — retrieve account info</li>
 *   <li>{@code POST /api/accounts/{accountNumber}/debit} — debit balance</li>
 *   <li>{@code POST /api/accounts/{accountNumber}/credit} — credit balance</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.account-service-url}")
    private String accountServiceUrl;

    /**
     * Retrieve account information for the given account number.
     *
     * @param accountNumber the account number
     * @return the account DTO
     * @throws AccountServiceException if the call fails or account-service returns an error
     */
    public AccountDto getAccount(String accountNumber) {
        String url = accountServiceUrl + "/api/accounts/" + accountNumber;
        log.debug("Fetching account info from: {}", url);
        try {
            ResponseEntity<AccountDto> resp = restTemplate.getForEntity(url, AccountDto.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new AccountServiceException("Account not found: " + accountNumber);
            }
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error fetching account {}: {}", accountNumber, ex.getMessage());
            throw new AccountServiceException("Unable to reach account-service: " + ex.getMessage(), ex);
        }
    }

    /**
     * Debit an account by the specified amount.
     *
     * @param accountNumber  the account number
     * @param amount          the amount to debit
     * @param referenceNumber correlation id for audit
     * @return the balance update response containing pre/post balances
     * @throws AccountServiceException on failure
     */
    public BalanceUpdateResponse debit(String accountNumber, BigDecimal amount, String referenceNumber) {
        String url = accountServiceUrl + "/api/accounts/" + accountNumber + "/debit";
        return postBalanceUpdate(url, amount, referenceNumber);
    }

    /**
     * Credit an account by the specified amount.
     *
     * @param accountNumber  the account number
     * @param amount          the amount to credit
     * @param referenceNumber correlation id for audit
     * @return the balance update response containing pre/post balances
     * @throws AccountServiceException on failure
     */
    public BalanceUpdateResponse credit(String accountNumber, BigDecimal amount, String referenceNumber) {
        String url = accountServiceUrl + "/api/accounts/" + accountNumber + "/credit";
        return postBalanceUpdate(url, amount, referenceNumber);
    }

    private BalanceUpdateResponse postBalanceUpdate(String url, BigDecimal amount, String referenceNumber) {
        log.debug("Posting balance update to {} (amount={}, ref={})", url, amount, referenceNumber);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            BalanceUpdateRequest body = BalanceUpdateRequest.builder()
                    .amount(amount)
                    .referenceNumber(referenceNumber)
                    .remark("Initiated by transaction-service")
                    .build();
            HttpEntity<BalanceUpdateRequest> request = new HttpEntity<>(body, headers);
            ResponseEntity<BalanceUpdateResponse> resp = restTemplate.exchange(
                    url, HttpMethod.POST, request, BalanceUpdateResponse.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new AccountServiceException("account-service returned non-success status: " + resp.getStatusCode());
            }
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Balance update call failed: {}", ex.getMessage());
            throw new AccountServiceException("Balance update failed: " + ex.getMessage(), ex);
        }
    }
}
