package com.pipeline.platform.pipeline.api.mapper;

import com.pipeline.platform.pipeline.api.request.PipelineRunLogRequest;
import com.pipeline.platform.pipeline.api.response.PipelineRunLogResponse;
import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.pipeline.api.request.LatestProjectPipelineRunLogRequest;
import com.pipeline.platform.pipeline.application.command.PipelineRunLogCommand;
import com.pipeline.platform.pipeline.application.command.LatestProjectPipelineRunLogCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunLogView;
import org.springframework.stereotype.Component;

@Component
public class PipelineRunLogResponseMapper {

    public PipelineRunLogCommand toCommand(PipelineRunLogRequest request) {
        return new PipelineRunLogCommand(request.pipelineRunId());
    }

    public LatestProjectPipelineRunLogCommand toCommand(LatestProjectPipelineRunLogRequest request) {
        return new LatestProjectPipelineRunLogCommand(request.projectId(), request.pipelineId());
    }

    public PipelineRunLogResponse toResponse(PipelineRunLogView view) {
        return new PipelineRunLogResponse(
                view.id(),
                view.pipelineRunId(),
                view.externalLogUrl(),
                view.logExcerpt(),
                view.logSizeBytes(),
                view.fetchedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public PipelineRunLogResponse toFullResponse(Long pipelineRunId, JenkinsConsoleLog consoleLog) {
        return new PipelineRunLogResponse(
                null,
                pipelineRunId,
                consoleLog.externalUrl(),
                consoleLog.text(),
                consoleLog.sizeBytes(),
                null,
                null,
                null
        );
    }
}
