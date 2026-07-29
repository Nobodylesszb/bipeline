package com.pipeline.platform.project.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.project.domain.ProjectGitRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repositories")
class ProjectGitRepositoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "remote_path", nullable = false, length = 500)
    private String remotePath;

    @Column(name = "remote_url", nullable = false, length = 1000)
    private String remoteUrl;

    @Column(name = "default_branch", nullable = false, length = 200)
    private String defaultBranch;

    @Column(name = "context_directory", nullable = false, length = 500)
    private String contextDirectory;

    @Column(name = "last_resolved_revision", length = 200)
    private String lastResolvedRevision;

    @Column(name = "last_fetched_at")
    private OffsetDateTime lastFetchedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProjectGitRepositoryJpaEntity() {
    }

    static ProjectGitRepositoryJpaEntity from(ProjectGitRepository repository) {
        ProjectGitRepositoryJpaEntity entity = new ProjectGitRepositoryJpaEntity();
        entity.id = repository.id();
        entity.projectId = repository.projectId();
        entity.remotePath = repository.remotePath();
        entity.remoteUrl = repository.remoteUrl();
        entity.defaultBranch = repository.defaultBranch();
        entity.contextDirectory = repository.contextDirectory();
        entity.lastResolvedRevision = repository.lastResolvedRevision();
        entity.lastFetchedAt = repository.lastFetchedAt();
        entity.createdAt = repository.createdAt();
        entity.updatedAt = repository.updatedAt();
        return entity;
    }

    ProjectGitRepository toDomain() {
        return new ProjectGitRepository(
                id,
                projectId,
                remotePath,
                remoteUrl,
                defaultBranch,
                contextDirectory,
                lastResolvedRevision,
                lastFetchedAt,
                createdAt,
                updatedAt
        );
    }
}
