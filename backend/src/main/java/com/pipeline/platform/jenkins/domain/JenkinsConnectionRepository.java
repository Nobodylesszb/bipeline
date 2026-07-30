package com.pipeline.platform.jenkins.domain;

import java.util.List;
import java.util.Optional;

public interface JenkinsConnectionRepository {

    boolean existsByName(String name);

    JenkinsConnection save(JenkinsConnection connection);

    Optional<JenkinsConnection> findById(Long id);

    List<JenkinsConnection> findAll();
}
