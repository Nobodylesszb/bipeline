package com.pipeline.platform.project.infrastructure.persistence;

import java.util.Optional;

import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectGitRepositoryRepository implements ProjectGitRepositoryRepository {

    @Autowired
    private SpringDataProjectGitRepositoryRepository springDataRepository;

    @Override
    public ProjectGitRepository save(ProjectGitRepository repository) {
        return springDataRepository.save(ProjectGitRepositoryJpaEntity.from(repository)).toDomain();
    }

    @Override
    public Optional<ProjectGitRepository> findByProjectId(Long projectId) {
        return springDataRepository.findByProjectId(projectId)
                .map(ProjectGitRepositoryJpaEntity::toDomain);
    }
}
