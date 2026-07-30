package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.pipeline.application.command.LatestProjectPipelineRunLogCommand;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LatestPipelineRunFinder {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    public PipelineRun find(LatestProjectPipelineRunLogCommand command) {
        if (command.pipelineId() == null) {
            return pipelineRunRepository.findLatestByProjectId(command.projectId())
                    .orElseThrow(this::notFound);
        }
        return pipelineRunRepository.findLatestByProjectIdAndPipelineId(command.projectId(), command.pipelineId())
                .orElseThrow(this::notFound);
    }

    private BusinessException notFound() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Latest pipeline run not found"
        );
    }
}
