package org.example.exception;


public class DatabaseException extends UserServiceException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
