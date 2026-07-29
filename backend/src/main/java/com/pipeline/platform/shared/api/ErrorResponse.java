package com.pipeline.platform.shared.api;

import java.util.Map;

public record ErrorResponse(
        int code,
        String message,
        Map<String, Object> details,
        String traceId
) {
}
