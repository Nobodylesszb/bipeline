package com.pipeline.platform.pipeline.api.response;

import java.time.OffsetDateTime;

import com.pipeline.platform.pipeline.domain.PipelineRunStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线运行响应")
public record PipelineRunResponse(
        @Schema(description = "流水线运行 ID", example = "1")
        Long id,

        @Schema(description = "流水线 ID", example = "1")
        Long pipelineId,

        @Schema(description = "项目 ID", example = "1")
        Long projectId,

        @Schema(description = "Jenkins 连接 ID", example = "2")
        Long jenkinsConnectionId,

        @Schema(description = "运行编号", example = "1")
        int runNumber,

        @Schema(description = "运行状态", example = "RUNNING")
        PipelineRunStatus status,

        @Schema(description = "触发方式", example = "MANUAL")
        TriggerType triggerType,

        @Schema(description = "运行分支", example = "master")
        String branch,

        @Schema(description = "提交 SHA")
        String commitSha,

        @Schema(description = "Jenkins Job 名称", example = "pipeline-1-1-main-ci")
        String jenkinsJobName,

        @Schema(description = "Jenkins Queue URL")
        String jenkinsQueueUrl,

        @Schema(description = "Jenkins Build Number", example = "1")
        Integer jenkinsBuildNumber,

        OffsetDateTime startedAt,

        OffsetDateTime finishedAt,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}
