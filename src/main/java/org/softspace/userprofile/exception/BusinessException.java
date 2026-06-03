package org.softspace.userprofile.exception;

import lombok.Getter;
import org.softspace.userprofile.controller.error.ErrorCode;
import org.springframework.core.NestedRuntimeException;

import java.util.Map;

@Getter
public abstract class BusinessException extends NestedRuntimeException {

    private final transient Map<String, Object> details;

    public BusinessException(String message, Map<String, Object> details) {
        super(message);
        this.details = details == null ? Map.of() : details;
    }

    public BusinessException(String message, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.details = details == null ? Map.of() : details;
    }

    public abstract ErrorCode getErrorCode();
}
