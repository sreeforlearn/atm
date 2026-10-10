package com.atm.transaction.exception;

/**
 * Thrown when an account has insufficient funds to satisfy a transaction.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
