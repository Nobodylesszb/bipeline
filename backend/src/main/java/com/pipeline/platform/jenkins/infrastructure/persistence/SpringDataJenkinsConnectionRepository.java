package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJenkinsConnectionRepository extends JpaRepository<JenkinsConnectionJpaEntity, Long> {

    boolean existsByName(String name);

    List<JenkinsConnectionJpaEntity> findAllByOrderByCreatedAtDesc();
}
