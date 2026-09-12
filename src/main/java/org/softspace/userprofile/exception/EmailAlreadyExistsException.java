package org.softspace.userprofile.exception;

import org.softspace.userprofile.controller.error.ErrorCode;

import java.util.Map;

public class EmailAlreadyExistsException extends BusinessException{
    public EmailAlreadyExistsException(String message, Map<String, Object> details) {
        super(message, details);
    }

    public EmailAlreadyExistsException(String message, Throwable cause, Map<String, Object> details) {
        super(message, cause, details);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.EMAIL_ALREADY_EXISTS;
    }
}
