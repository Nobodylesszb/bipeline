package com.pipeline.platform.pipeline.application.service;

import java.util.List;

import com.pipeline.platform.pipeline.application.command.ListPipelinesCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListPipelinesService {

    @Autowired
    private PipelineRepository pipelineRepository;

    @Transactional(readOnly = true)
    public List<PipelineView> findAll(ListPipelinesCommand command) {
        return pipelineRepository.findByProjectId(command.projectId())
                .stream()
                .map(PipelineView::from)
                .toList();
    }
}
