package com.atm.card.exception;

/**
 * Thrown when a card lookup fails to find a matching record.
 */
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String message) {
        super(message);
    }
}
