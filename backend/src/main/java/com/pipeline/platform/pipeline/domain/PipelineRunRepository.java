package com.pipeline.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;

public interface PipelineRunRepository {

    PipelineRun save(PipelineRun pipelineRun);

    Optional<PipelineRun> findById(Long id);

    Optional<PipelineRun> findLatestByProjectId(Long projectId);

    Optional<PipelineRun> findLatestByProjectIdAndPipelineId(Long projectId, Long pipelineId);

    List<PipelineRun> findByPipelineId(Long pipelineId);

    int nextRunNumber(Long pipelineId);
}
