package com.pipeline.platform.pipeline.api.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线响应")
public record PipelineResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        PipelineStatus status,
        TriggerType triggerType,
        String branchName,
        int version,
        List<PipelineStageResponse> stages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
