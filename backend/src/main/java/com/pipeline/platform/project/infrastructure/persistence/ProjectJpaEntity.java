package com.pipeline.platform.project.infrastructure.persistence;

import java.time.OffsetDateTime;

import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
class ProjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "code_source_id", nullable = false)
    private Long codeSourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProjectJpaEntity() {
    }

    static ProjectJpaEntity from(Project project) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.id = project.id();
        entity.name = project.name();
        entity.description = project.description();
        entity.codeSourceId = project.codeSourceId();
        entity.status = project.status();
        entity.createdAt = project.createdAt();
        entity.updatedAt = project.updatedAt();
        return entity;
    }

    Project toDomain() {
        return new Project(
                id,
                name,
                description,
                codeSourceId,
                status,
                createdAt,
                updatedAt
        );
    }
}
