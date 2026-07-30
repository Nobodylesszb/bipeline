package com.pipeline.platform.pipeline.api.response;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "流水线运行日志响应")
public record PipelineRunLogResponse(
        @Schema(description = "日志记录 ID", example = "1")
        Long id,

        @Schema(description = "流水线运行 ID", example = "6")
        Long pipelineRunId,

        @Schema(description = "Jenkins 原始日志地址")
        String externalLogUrl,

        @Schema(description = "最后一段日志，默认最后 200 行")
        String logExcerpt,

        @Schema(description = "Jenkins 原始日志大小，单位 byte", example = "12000")
        long logSizeBytes,

        @Schema(description = "本次拉取时间")
        OffsetDateTime fetchedAt,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}
