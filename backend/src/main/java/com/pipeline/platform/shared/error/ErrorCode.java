package com.pipeline.platform.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT),
    CODE_SOURCE_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST),
    REPOSITORY_NOT_ACCESSIBLE(HttpStatus.BAD_REQUEST),
    PIPELINE_NOT_RUNNABLE(HttpStatus.CONFLICT),
    RUN_ALREADY_TERMINAL(HttpStatus.CONFLICT),
    EXECUTION_ENGINE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    PLUGIN_CONTRACT_INVALID(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
