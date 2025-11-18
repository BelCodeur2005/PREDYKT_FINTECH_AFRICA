// UnbalancedEntryException.java
package com.predykt.accounting.exception;

public class UnbalancedEntryException extends RuntimeException {
    public UnbalancedEntryException(String message) {
        super(message);
    }
}