package com.tizvagt.api.auth;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email is already registered: " + email);
    }
}
