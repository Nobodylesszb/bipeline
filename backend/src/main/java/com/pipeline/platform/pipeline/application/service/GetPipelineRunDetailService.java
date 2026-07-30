package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.pipeline.application.command.PipelineRunDetailCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetPipelineRunDetailService {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Transactional(readOnly = true)
    public PipelineRunView get(PipelineRunDetailCommand command) {
        return pipelineRunRepository.findById(command.pipelineRunId())
                .map(PipelineRunView::from)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline run not found"
                ));
    }
}
