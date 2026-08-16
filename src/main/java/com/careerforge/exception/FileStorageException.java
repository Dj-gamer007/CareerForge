package com.careerforge.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a file upload fails validation (wrong type, oversized, path traversal)
 * or when a stored file cannot be read from disk.
 */
public class FileStorageException extends ApiException {

    public FileStorageException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST);
    }
}
