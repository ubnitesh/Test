package org.oransc.rappmanager.models.exception;

import lombok.Getter;

@Getter
public class RappHandlerException extends RuntimeException {

    private final int statusCode;

    public RappHandlerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
