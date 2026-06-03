package org.softspace.userprofile.exception;

import org.softspace.userprofile.controller.error.ErrorCode;

import java.util.Map;

public class UserProfileNotFoundException extends BusinessException {
    public UserProfileNotFoundException(String message, Map<String, Object> details) {
        super(message, details);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.USER_PROFILE_NOT_FOUND;
    }
}
