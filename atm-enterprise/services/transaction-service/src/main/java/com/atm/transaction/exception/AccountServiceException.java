package com.atm.transaction.exception;

/**
 * Thrown when calls to the account-service fail (network, 4xx/5xx, etc.).
 */
public class AccountServiceException extends RuntimeException {

    public AccountServiceException(String message) {
        super(message);
    }

    public AccountServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
