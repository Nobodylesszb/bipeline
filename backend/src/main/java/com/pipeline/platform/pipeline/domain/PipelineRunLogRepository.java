package com.pipeline.platform.pipeline.domain;

import java.util.Optional;

public interface PipelineRunLogRepository {

    PipelineRunLog save(PipelineRunLog pipelineRunLog);

    Optional<PipelineRunLog> findLatestByPipelineRunId(Long pipelineRunId);
}
