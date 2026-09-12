package org.softspace.userprofile.exception;

import org.softspace.userprofile.controller.error.ErrorCode;

import java.util.Map;

public class ValidationException extends BusinessException {
    public ValidationException(String message, Map<String, Object> details) {
        super(message, details);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
