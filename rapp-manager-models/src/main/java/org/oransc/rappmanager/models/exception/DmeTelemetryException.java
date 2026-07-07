package org.oransc.rappmanager.models.exception;

import lombok.Getter;

@Getter
public class DmeTelemetryException extends RuntimeException {

    private final int statusCode;

    public DmeTelemetryException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
