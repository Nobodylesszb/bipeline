package com.pipeline.platform.jenkins.domain;

import java.util.Optional;

public interface JenkinsJobRepository {

    JenkinsJob save(JenkinsJob jenkinsJob);

    Optional<JenkinsJob> findByPipelineIdAndJenkinsConnectionId(Long pipelineId, Long jenkinsConnectionId);
}
