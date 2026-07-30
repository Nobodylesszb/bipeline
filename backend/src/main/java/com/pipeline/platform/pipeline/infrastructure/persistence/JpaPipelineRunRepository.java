package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPipelineRunRepository implements PipelineRunRepository {

    @Autowired
    private SpringDataPipelineRunRepository springDataRepository;

    @Override
    public PipelineRun save(PipelineRun pipelineRun) {
        return springDataRepository.save(PipelineRunJpaEntity.from(pipelineRun)).toDomain();
    }

    @Override
    public Optional<PipelineRun> findById(Long id) {
        return springDataRepository.findById(id).map(PipelineRunJpaEntity::toDomain);
    }

    @Override
    public Optional<PipelineRun> findLatestByProjectId(Long projectId) {
        return springDataRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .map(PipelineRunJpaEntity::toDomain);
    }

    @Override
    public Optional<PipelineRun> findLatestByProjectIdAndPipelineId(Long projectId, Long pipelineId) {
        return springDataRepository.findFirstByProjectIdAndPipelineIdOrderByCreatedAtDesc(projectId, pipelineId)
                .map(PipelineRunJpaEntity::toDomain);
    }

    @Override
    public List<PipelineRun> findByPipelineId(Long pipelineId) {
        return springDataRepository.findByPipelineIdOrderByRunNumberDesc(pipelineId)
                .stream()
                .map(PipelineRunJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int nextRunNumber(Long pipelineId) {
        return springDataRepository.maxRunNumberByPipelineId(pipelineId) + 1;
    }
}
