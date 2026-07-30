package com.pipeline.platform.pipeline.api.request;

import java.util.List;
import java.util.Map;

import com.pipeline.platform.pipeline.domain.StepType;
import com.pipeline.platform.pipeline.domain.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "创建流水线请求")
public record CreatePipelineRequest(
        @NotNull(message = "Project id is required")
        @Schema(description = "项目 ID", example = "1")
        Long projectId,

        @NotBlank(message = "Pipeline name is required")
        @Size(max = 120, message = "Pipeline name must be less than 120 characters")
        @Schema(description = "流水线名称", example = "main-ci")
        String name,

        @Size(max = 1000, message = "Pipeline description must be less than 1000 characters")
        @Schema(description = "流水线描述", example = "主分支 CI")
        String description,

        @NotNull(message = "Trigger type is required")
        @Schema(description = "触发方式", example = "MANUAL")
        TriggerType triggerType,

        @Schema(description = "构建分支。为空时使用项目绑定仓库默认分支", example = "master")
        String branchName,

        @Valid
        @NotEmpty(message = "Pipeline stages are required")
        @Schema(description = "流水线阶段")
        List<StageRequest> stages
) {

    @Schema(description = "流水线阶段请求")
    public record StageRequest(
            @NotBlank(message = "Stage name is required")
            @Schema(description = "阶段唯一名称", example = "default")
            String name,

            @Schema(description = "阶段展示名称", example = "默认阶段")
            String displayName,

            @Valid
            @NotEmpty(message = "Stage steps are required")
            @Schema(description = "阶段步骤")
            List<StepRequest> steps
    ) {
    }

    @Schema(description = "流水线步骤请求")
    public record StepRequest(
            @NotNull(message = "Step type is required")
            @Schema(description = "步骤类型", example = "SHELL")
            StepType type,

            @NotBlank(message = "Step name is required")
            @Schema(description = "步骤唯一名称", example = "test")
            String name,

            @Schema(description = "步骤展示名称", example = "运行测试")
            String displayName,

            @Schema(description = "步骤配置 JSON", example = "{\"command\":\"mvn test\"}")
            Map<String, Object> config
    ) {
    }
}
