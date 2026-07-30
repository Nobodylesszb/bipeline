package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataPipelineRunRepository extends JpaRepository<PipelineRunJpaEntity, Long> {

    List<PipelineRunJpaEntity> findByPipelineIdOrderByRunNumberDesc(Long pipelineId);

    Optional<PipelineRunJpaEntity> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<PipelineRunJpaEntity> findFirstByProjectIdAndPipelineIdOrderByCreatedAtDesc(Long projectId, Long pipelineId);

    @Query("select coalesce(max(run.runNumber), 0) from PipelineRunJpaEntity run where run.pipelineId = :pipelineId")
    int maxRunNumberByPipelineId(Long pipelineId);
}
