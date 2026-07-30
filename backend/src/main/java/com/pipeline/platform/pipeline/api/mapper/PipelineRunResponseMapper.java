package com.pipeline.platform.pipeline.api.mapper;

import com.pipeline.platform.pipeline.api.request.StartPipelineRunRequest;
import com.pipeline.platform.pipeline.api.response.PipelineRunResponse;
import com.pipeline.platform.pipeline.application.command.StartPipelineRunCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import org.springframework.stereotype.Component;

@Component
public class PipelineRunResponseMapper {

    public StartPipelineRunCommand toCommand(StartPipelineRunRequest request) {
        return new StartPipelineRunCommand(
                request.pipelineId(),
                request.jenkinsConnectionId(),
                request.branch(),
                request.commitSha()
        );
    }

    public PipelineRunResponse toResponse(PipelineRunView view) {
        return new PipelineRunResponse(
                view.id(),
                view.pipelineId(),
                view.projectId(),
                view.jenkinsConnectionId(),
                view.runNumber(),
                view.status(),
                view.triggerType(),
                view.branch(),
                view.commitSha(),
                view.jenkinsJobName(),
                view.jenkinsQueueUrl(),
                view.jenkinsBuildNumber(),
                view.startedAt(),
                view.finishedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
