package com.atm.transaction.exception;

/**
 * Thrown when a transaction lookup fails to find a matching record.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
