package com.momentive.backend.common.exception;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }
}
