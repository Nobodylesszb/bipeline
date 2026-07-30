package com.pipeline.platform.pipeline.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPipelineRepository extends JpaRepository<PipelineJpaEntity, Long> {

    boolean existsByProjectIdAndName(Long projectId, String name);

    List<PipelineJpaEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
