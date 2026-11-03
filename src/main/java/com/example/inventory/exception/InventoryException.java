package com.example.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryException extends RuntimeException {

    private final HttpStatus status;

    public InventoryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
