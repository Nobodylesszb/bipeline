package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPipelineRepository implements PipelineRepository {

    @Autowired
    private SpringDataPipelineRepository springDataRepository;

    @Override
    public boolean existsByProjectIdAndName(Long projectId, String name) {
        return springDataRepository.existsByProjectIdAndName(projectId, name);
    }

    @Override
    public Pipeline save(Pipeline pipeline) {
        return springDataRepository.save(PipelineJpaEntity.from(pipeline)).toDomain();
    }

    @Override
    public Optional<Pipeline> findById(Long id) {
        return springDataRepository.findById(id).map(PipelineJpaEntity::toDomain);
    }

    @Override
    public List<Pipeline> findByProjectId(Long projectId) {
        return springDataRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(PipelineJpaEntity::toDomain)
                .toList();
    }
}
