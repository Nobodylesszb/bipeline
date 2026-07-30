package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.Optional;

import com.pipeline.platform.pipeline.domain.PipelineRunLog;
import com.pipeline.platform.pipeline.domain.PipelineRunLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPipelineRunLogRepository implements PipelineRunLogRepository {

    @Autowired
    private SpringDataPipelineRunLogRepository springDataRepository;

    @Override
    public PipelineRunLog save(PipelineRunLog pipelineRunLog) {
        return springDataRepository.save(PipelineRunLogJpaEntity.from(pipelineRunLog)).toDomain();
    }

    @Override
    public Optional<PipelineRunLog> findLatestByPipelineRunId(Long pipelineRunId) {
        return springDataRepository.findFirstByPipelineRunIdOrderByFetchedAtDesc(pipelineRunId)
                .map(PipelineRunLogJpaEntity::toDomain);
    }
}
