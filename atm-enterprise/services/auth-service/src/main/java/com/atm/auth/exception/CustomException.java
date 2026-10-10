package com.atm.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain-level exception carrying an HTTP status code.
 */
@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CustomException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
