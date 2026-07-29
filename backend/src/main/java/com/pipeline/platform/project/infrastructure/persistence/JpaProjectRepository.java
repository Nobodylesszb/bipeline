package com.pipeline.platform.project.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectRepository implements ProjectRepository {

    @Autowired
    private SpringDataProjectRepository springDataRepository;

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public Project save(Project project) {
        return springDataRepository.save(ProjectJpaEntity.from(project)).toDomain();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return springDataRepository.findById(id).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return springDataRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ProjectJpaEntity::toDomain)
                .toList();
    }
}
