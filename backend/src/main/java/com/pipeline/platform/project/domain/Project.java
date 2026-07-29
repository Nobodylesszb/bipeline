package com.pipeline.platform.project.domain;

import java.time.OffsetDateTime;

public record Project(
        Long id,
        String name,
        String description,
        Long codeSourceId,
        ProjectStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static Project create(
            String name,
            String description,
            Long codeSourceId,
            OffsetDateTime now
    ) {
        return new Project(
                null,
                name.trim(),
                blankToNull(description),
                codeSourceId,
                ProjectStatus.ACTIVE,
                now,
                now
        );
    }

    public boolean usesCodeSource(Long requestedCodeSourceId) {
        return codeSourceId.equals(requestedCodeSourceId);
    }

    public boolean active() {
        return status == ProjectStatus.ACTIVE;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
