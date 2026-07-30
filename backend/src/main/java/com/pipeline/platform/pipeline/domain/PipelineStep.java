package com.pipeline.platform.pipeline.domain;

import java.time.OffsetDateTime;

public record PipelineStep(
        Long id,
        Long pipelineId,
        Long stageId,
        String stepKey,
        String name,
        String displayName,
        StepType type,
        int sortOrder,
        String configJson,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PipelineStep create(
            String name,
            String displayName,
            StepType type,
            int sortOrder,
            String configJson,
            OffsetDateTime now
    ) {
        String normalizedName = normalizeRequired(name);
        return new PipelineStep(
                null,
                null,
                null,
                normalizedName,
                normalizedName,
                defaultDisplayName(displayName, normalizedName),
                type,
                sortOrder,
                configJson,
                true,
                now,
                now
        );
    }

    public PipelineStep attach(Long pipelineId, Long stageId) {
        return new PipelineStep(
                id,
                pipelineId,
                stageId,
                stepKey,
                name,
                displayName,
                type,
                sortOrder,
                configJson,
                enabled,
                createdAt,
                updatedAt
        );
    }

    private static String normalizeRequired(String value) {
        return value.trim();
    }

    private static String defaultDisplayName(String displayName, String name) {
        if (displayName == null || displayName.isBlank()) {
            return name;
        }
        return displayName.trim();
    }
}
