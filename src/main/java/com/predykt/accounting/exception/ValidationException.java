// ValidationException.java
package com.predykt.accounting.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}