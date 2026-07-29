package com.pipeline.platform.source.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCodeSourceRepository extends JpaRepository<CodeSourceJpaEntity, UUID> {

    boolean existsByName(String name);

    List<CodeSourceJpaEntity> findAllByOrderByCreatedAtDesc();
}
