package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.pipeline.application.command.PipelineRunLogCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunLogView;
import com.pipeline.platform.pipeline.domain.PipelineRunLogRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetPipelineRunLogService {

    @Autowired
    private PipelineRunLogRepository pipelineRunLogRepository;

    @Transactional(readOnly = true)
    public PipelineRunLogView get(PipelineRunLogCommand command) {
        return pipelineRunLogRepository.findLatestByPipelineRunId(command.pipelineRunId())
                .map(PipelineRunLogView::from)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline run log not found"
                ));
    }
}
