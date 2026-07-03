package org.oransc.rappmanager.models.exception;

import lombok.Getter;
import org.oransc.rappmanager.models.r1.ProblemDetails;

@Getter
public class R1ApiException extends RuntimeException {

    private final ProblemDetails problemDetails;

    public R1ApiException(ProblemDetails problemDetails) {
        super(problemDetails.getDetail());
        this.problemDetails = problemDetails;
    }
}
