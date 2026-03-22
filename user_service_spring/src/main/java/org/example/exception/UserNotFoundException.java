package org.example.exception;


public class UserNotFoundException extends UserServiceException {

    public UserNotFoundException(Long id) {
        super("Пользователь с id " + id + " не найден.");
    }
}
