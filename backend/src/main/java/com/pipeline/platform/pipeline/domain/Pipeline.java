package com.pipeline.platform.pipeline.domain;

import java.time.OffsetDateTime;
import java.util.List;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;

public record Pipeline(
        Long id,
        Long projectId,
        String name,
        String description,
        PipelineStatus status,
        TriggerType triggerType,
        String branchName,
        int version,
        List<PipelineStage> stages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static Pipeline create(
            Long projectId,
            String name,
            String description,
            TriggerType triggerType,
            String branchName,
            List<PipelineStage> stages,
            OffsetDateTime now
    ) {
        return new Pipeline(
                null,
                projectId,
                name.trim(),
                blankToNull(description),
                PipelineStatus.DRAFT,
                triggerType,
                branchName.trim(),
                1,
                List.copyOf(stages),
                now,
                now
        );
    }

    public Pipeline activate(OffsetDateTime now) {
        if (status != PipelineStatus.DRAFT && status != PipelineStatus.DISABLED) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Pipeline cannot be activated from current status"
            );
        }
        return withStatus(PipelineStatus.ACTIVE, now);
    }

    public Pipeline disable(OffsetDateTime now) {
        if (status != PipelineStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Only active pipeline can be disabled"
            );
        }
        return withStatus(PipelineStatus.DISABLED, now);
    }

    private Pipeline withStatus(PipelineStatus newStatus, OffsetDateTime now) {
        return new Pipeline(
                id,
                projectId,
                name,
                description,
                newStatus,
                triggerType,
                branchName,
                version,
                stages,
                createdAt,
                now
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
