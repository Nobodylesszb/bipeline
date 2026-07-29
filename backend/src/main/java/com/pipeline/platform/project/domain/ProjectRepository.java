package com.pipeline.platform.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    boolean existsByName(String name);

    Project save(Project project);

    Optional<Project> findById(Long id);

    List<Project> findAll();
}
