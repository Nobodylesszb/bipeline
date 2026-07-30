package com.pipeline.platform.pipeline.application.service;

import java.util.List;

import com.pipeline.platform.pipeline.application.command.CreatePipelineCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.pipeline.domain.PipelineStage;
import com.pipeline.platform.pipeline.domain.PipelineStep;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreatePipelineService {

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private ProjectPipelineGuard projectPipelineGuard;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public PipelineView create(CreatePipelineCommand command) {
        ProjectGitRepository projectRepository = projectPipelineGuard.requireActiveProjectWithRepository(command.projectId());
        if (pipelineRepository.existsByProjectIdAndName(command.projectId(), command.name())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Pipeline name already exists in project"
            );
        }

        Pipeline pipeline = Pipeline.create(
                command.projectId(),
                command.name(),
                command.description(),
                command.triggerType(),
                branchName(command.branchName(), projectRepository.defaultBranch()),
                toStages(command.stages()),
                clockProvider.now()
        );
        return PipelineView.from(pipelineRepository.save(pipeline));
    }

    private List<PipelineStage> toStages(List<CreatePipelineCommand.StageCommand> stageCommands) {
        if (stageCommands == null || stageCommands.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Pipeline must contain at least one stage"
            );
        }
        return stageCommands.stream()
                .map(stage -> PipelineStage.create(
                        stage.name(),
                        stage.displayName(),
                        stageCommands.indexOf(stage) + 1,
                        toSteps(stage.steps()),
                        clockProvider.now()
                ))
                .toList();
    }

    private List<PipelineStep> toSteps(List<CreatePipelineCommand.StepCommand> stepCommands) {
        if (stepCommands == null || stepCommands.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Pipeline stage must contain at least one step"
            );
        }
        return stepCommands.stream()
                .map(step -> PipelineStep.create(
                        step.name(),
                        step.displayName(),
                        step.type(),
                        stepCommands.indexOf(step) + 1,
                        step.configJson(),
                        clockProvider.now()
                ))
                .toList();
    }

    private String branchName(String requestedBranch, String projectDefaultBranch) {
        if (requestedBranch == null || requestedBranch.isBlank()) {
            return projectDefaultBranch;
        }
        return requestedBranch;
    }
}
