package com.pipeline.platform.project.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {

    boolean existsByName(String name);

    List<ProjectJpaEntity> findAllByOrderByCreatedAtDesc();
}
