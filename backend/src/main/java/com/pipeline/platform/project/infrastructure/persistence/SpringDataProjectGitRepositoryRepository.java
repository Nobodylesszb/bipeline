package com.pipeline.platform.project.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProjectGitRepositoryRepository extends JpaRepository<ProjectGitRepositoryJpaEntity, Long> {

    Optional<ProjectGitRepositoryJpaEntity> findByProjectId(Long projectId);
}
