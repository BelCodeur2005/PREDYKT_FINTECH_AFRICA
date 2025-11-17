// ResourceNotFoundException.java
package com.predykt.accounting.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// UnbalancedEntryException.java
package com.predykt.accounting.exception;

public class UnbalancedEntryException extends RuntimeException {
    public UnbalancedEntryException(String message) {
        super(message);
    }
}

// AccountingException.java
package com.predykt.accounting.exception;

public class AccountingException extends RuntimeException {
    public AccountingException(String message) {
        super(message);
    }
    
    public AccountingException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ValidationException.java
package com.predykt.accounting.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}