package com.pipeline.platform.pipeline.application.service;

import java.util.List;

import com.pipeline.platform.pipeline.application.command.ListPipelineRunsCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListPipelineRunsService {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Transactional(readOnly = true)
    public List<PipelineRunView> findAll(ListPipelineRunsCommand command) {
        return pipelineRunRepository.findByPipelineId(command.pipelineId())
                .stream()
                .map(PipelineRunView::from)
                .toList();
    }
}
