package org.oransc.rappmanager.rest;

import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.models.exception.DmeTelemetryException;
import org.oransc.rappmanager.models.exception.ErrorResponse;
import org.oransc.rappmanager.models.exception.R1ApiException;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.r1.ProblemDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionControllerHandler {

    @ExceptionHandler(DmeTelemetryException.class)
    public ResponseEntity<ErrorResponse> handleDmeTelemetryException(DmeTelemetryException exception) {
        log.warn("DME telemetry error: {}", exception.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .message(exception.getMessage())
                .status(exception.getStatusCode())
                .build();
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }

    @ExceptionHandler(RappHandlerException.class)
    public ResponseEntity<ErrorResponse> handleRappException(RappHandlerException exception) {
        log.warn("rApp handler error: {}", exception.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .message(exception.getMessage())
                .status(exception.getStatusCode())
                .build();
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }

    @ExceptionHandler(R1ApiException.class)
    public ResponseEntity<ProblemDetails> handleR1ApiException(R1ApiException exception) {
        ProblemDetails problemDetails = exception.getProblemDetails();
        log.warn("R1 API error: {}", problemDetails.getDetail());
        return ResponseEntity.status(problemDetails.getStatus()).body(problemDetails);
    }
}
