package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJenkinsJobRepository extends JpaRepository<JenkinsJobJpaEntity, Long> {

    Optional<JenkinsJobJpaEntity> findByPipelineIdAndJenkinsConnectionId(Long pipelineId, Long jenkinsConnectionId);
}
