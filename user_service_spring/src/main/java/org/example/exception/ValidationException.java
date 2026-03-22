package org.example.exception;


public class ValidationException extends UserServiceException {

    public ValidationException(String message) {
        super(message);
    }
}
