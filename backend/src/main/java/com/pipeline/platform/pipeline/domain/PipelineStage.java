package com.pipeline.platform.pipeline.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record PipelineStage(
        Long id,
        Long pipelineId,
        String name,
        String displayName,
        int sortOrder,
        List<PipelineStep> steps,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PipelineStage create(
            String name,
            String displayName,
            int sortOrder,
            List<PipelineStep> steps,
            OffsetDateTime now
    ) {
        String normalizedName = normalizeRequired(name);
        return new PipelineStage(
                null,
                null,
                normalizedName,
                defaultDisplayName(displayName, normalizedName),
                sortOrder,
                List.copyOf(steps),
                now,
                now
        );
    }

    public PipelineStage attach(Long pipelineId, Long stageId) {
        return new PipelineStage(
                stageId,
                pipelineId,
                name,
                displayName,
                sortOrder,
                steps.stream()
                        .map(step -> step.attach(pipelineId, stageId))
                        .toList(),
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
