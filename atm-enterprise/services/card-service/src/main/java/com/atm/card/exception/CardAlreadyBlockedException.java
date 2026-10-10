package com.atm.card.exception;

/**
 * Thrown when attempting to block a card that is already blocked.
 */
public class CardAlreadyBlockedException extends RuntimeException {

    public CardAlreadyBlockedException(String message) {
        super(message);
    }
}
