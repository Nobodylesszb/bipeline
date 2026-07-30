package com.pipeline.platform.pipeline.application.command;

import java.util.List;

import com.pipeline.platform.pipeline.domain.StepType;
import com.pipeline.platform.pipeline.domain.TriggerType;

public record CreatePipelineCommand(
        Long projectId,
        String name,
        String description,
        TriggerType triggerType,
        String branchName,
        List<StageCommand> stages
) {

    public record StageCommand(
            String name,
            String displayName,
            List<StepCommand> steps
    ) {
    }

    public record StepCommand(
            StepType type,
            String name,
            String displayName,
            String configJson
    ) {
    }
}
