package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.pipeline.application.command.ChangePipelineStatusCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChangePipelineStatusService {

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public PipelineView activate(ChangePipelineStatusCommand command) {
        Pipeline pipeline = requirePipeline(command.pipelineId());
        return PipelineView.from(pipelineRepository.save(pipeline.activate(clockProvider.now())));
    }

    @Transactional(rollbackFor = Exception.class)
    public PipelineView disable(ChangePipelineStatusCommand command) {
        Pipeline pipeline = requirePipeline(command.pipelineId());
        return PipelineView.from(pipelineRepository.save(pipeline.disable(clockProvider.now())));
    }

    private Pipeline requirePipeline(Long pipelineId) {
        return pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline not found"
                ));
    }
}
