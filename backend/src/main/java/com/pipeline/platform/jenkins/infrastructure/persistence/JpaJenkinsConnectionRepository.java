package com.pipeline.platform.jenkins.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaJenkinsConnectionRepository implements JenkinsConnectionRepository {

    @Autowired
    private SpringDataJenkinsConnectionRepository springDataRepository;

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public JenkinsConnection save(JenkinsConnection connection) {
        return springDataRepository.save(JenkinsConnectionJpaEntity.from(connection)).toDomain();
    }

    @Override
    public Optional<JenkinsConnection> findById(Long id) {
        return springDataRepository.findById(id).map(JenkinsConnectionJpaEntity::toDomain);
    }

    @Override
    public List<JenkinsConnection> findAll() {
        return springDataRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(JenkinsConnectionJpaEntity::toDomain)
                .toList();
    }
}
