package com.careerforge.exception;

import org.springframework.http.HttpStatus;

public class TokenRefreshException extends ApiException {

    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message), HttpStatus.FORBIDDEN);
    }
}
