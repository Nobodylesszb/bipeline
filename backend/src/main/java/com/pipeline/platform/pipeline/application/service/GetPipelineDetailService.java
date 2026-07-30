package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.pipeline.application.command.GetPipelineDetailCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetPipelineDetailService {

    @Autowired
    private PipelineRepository pipelineRepository;

    @Transactional(readOnly = true)
    public PipelineView get(GetPipelineDetailCommand command) {
        return pipelineRepository.findById(command.pipelineId())
                .map(PipelineView::from)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline not found"
                ));
    }
}
