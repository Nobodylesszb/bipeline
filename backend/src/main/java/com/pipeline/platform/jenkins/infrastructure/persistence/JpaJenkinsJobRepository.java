package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.util.Optional;

import com.pipeline.platform.jenkins.domain.JenkinsJob;
import com.pipeline.platform.jenkins.domain.JenkinsJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaJenkinsJobRepository implements JenkinsJobRepository {

    @Autowired
    private SpringDataJenkinsJobRepository springDataRepository;

    @Override
    public JenkinsJob save(JenkinsJob jenkinsJob) {
        return springDataRepository.save(JenkinsJobJpaEntity.from(jenkinsJob)).toDomain();
    }

    @Override
    public Optional<JenkinsJob> findByPipelineIdAndJenkinsConnectionId(Long pipelineId, Long jenkinsConnectionId) {
        return springDataRepository.findByPipelineIdAndJenkinsConnectionId(pipelineId, jenkinsConnectionId)
                .map(JenkinsJobJpaEntity::toDomain);
    }
}
