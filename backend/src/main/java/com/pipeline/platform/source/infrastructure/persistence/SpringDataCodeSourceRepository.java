package com.pipeline.platform.source.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCodeSourceRepository extends JpaRepository<CodeSourceJpaEntity, Long> {

    boolean existsByName(String name);

    List<CodeSourceJpaEntity> findAllByOrderByCreatedAtDesc();
}
