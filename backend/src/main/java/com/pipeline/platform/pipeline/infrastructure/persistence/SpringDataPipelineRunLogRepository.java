package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPipelineRunLogRepository extends JpaRepository<PipelineRunLogJpaEntity, Long> {

    Optional<PipelineRunLogJpaEntity> findFirstByPipelineRunIdOrderByFetchedAtDesc(Long pipelineRunId);
}
