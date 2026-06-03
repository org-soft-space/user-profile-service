package org.softspace.userprofile.dto.error;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String status,
        String message,
        Map<String, Object> details,
        String path,
        Instant timestamp,
        String traceId
) {
}
