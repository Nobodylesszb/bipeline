package com.pipeline.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;

public interface PipelineRepository {

    boolean existsByProjectIdAndName(Long projectId, String name);

    Pipeline save(Pipeline pipeline);

    Optional<Pipeline> findById(Long id);

    List<Pipeline> findByProjectId(Long projectId);
}
