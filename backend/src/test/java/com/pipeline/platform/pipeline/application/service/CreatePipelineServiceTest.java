package com.pipeline.platform.pipeline.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.pipeline.application.command.CreatePipelineCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.StepType;
import com.pipeline.platform.pipeline.domain.TriggerType;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CreatePipelineServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-29T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createsDraftPipelineForBoundProject() {
        InMemoryPipelineRepository pipelineRepository = new InMemoryPipelineRepository();
        CreatePipelineService service = createService(
                pipelineRepository,
                new InMemoryProjectRepository(project()),
                new InMemoryProjectGitRepositoryRepository(repositoryBinding())
        );

        PipelineView result = service.create(command(null));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(PipelineStatus.DRAFT);
        assertThat(result.branchName()).isEqualTo("master");
        assertThat(result.stages()).hasSize(1);
        assertThat(result.stages().get(0).steps()).hasSize(1);
    }

    @Test
    void rejectsPipelineWhenProjectRepositoryIsNotBound() {
        CreatePipelineService service = createService(
                new InMemoryPipelineRepository(),
                new InMemoryProjectRepository(project()),
                new InMemoryProjectGitRepositoryRepository(null)
        );

        assertThatThrownBy(() -> service.create(command("master")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    @Test
    void rejectsDuplicatePipelineNameInSameProject() {
        InMemoryPipelineRepository pipelineRepository = new InMemoryPipelineRepository();
        CreatePipelineService service = createService(
                pipelineRepository,
                new InMemoryProjectRepository(project()),
                new InMemoryProjectGitRepositoryRepository(repositoryBinding())
        );
        service.create(command("master"));

        assertThatThrownBy(() -> service.create(command("master")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    private CreatePipelineService createService(
            PipelineRepository pipelineRepository,
            ProjectRepository projectRepository,
            ProjectGitRepositoryRepository projectGitRepositoryRepository
    ) {
        ProjectPipelineGuard guard = new ProjectPipelineGuard();
        ReflectionTestUtils.setField(guard, "projectRepository", projectRepository);
        ReflectionTestUtils.setField(guard, "projectGitRepositoryRepository", projectGitRepositoryRepository);

        CreatePipelineService service = new CreatePipelineService();
        ReflectionTestUtils.setField(service, "pipelineRepository", pipelineRepository);
        ReflectionTestUtils.setField(service, "projectPipelineGuard", guard);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private CreatePipelineCommand command(String branchName) {
        return new CreatePipelineCommand(
                1L,
                "main-ci",
                "主分支 CI",
                TriggerType.MANUAL,
                branchName,
                List.of(new CreatePipelineCommand.StageCommand(
                        "default",
                        "默认阶段",
                        List.of(new CreatePipelineCommand.StepCommand(
                                StepType.SHELL,
                                "test",
                                "运行测试",
                                "{\"command\":\"mvn test\"}"
                        ))
                ))
        );
    }

    private Project project() {
        return Project.create(
                "hotel-link",
                "酒店供应链 CI 项目",
                1L,
                clockProvider.now()
        );
    }

    private ProjectGitRepository repositoryBinding() {
        return ProjectGitRepository.bind(
                1L,
                "bobo_3776/hotel_link_supply",
                "http://localhost:3000/bobo_3776/hotel_link_supply.git",
                "master",
                ".",
                "commit-master",
                clockProvider.now()
        );
    }

    private static class InMemoryPipelineRepository implements PipelineRepository {

        private final List<Pipeline> pipelines = new ArrayList<>();

        @Override
        public boolean existsByProjectIdAndName(Long projectId, String name) {
            return pipelines.stream()
                    .anyMatch(pipeline -> pipeline.projectId().equals(projectId)
                            && pipeline.name().equals(name));
        }

        @Override
        public Pipeline save(Pipeline pipeline) {
            Pipeline saved = pipeline.id() == null
                    ? new Pipeline(
                            1L,
                            pipeline.projectId(),
                            pipeline.name(),
                            pipeline.description(),
                            pipeline.status(),
                            pipeline.triggerType(),
                            pipeline.branchName(),
                            pipeline.version(),
                            pipeline.stages(),
                            pipeline.createdAt(),
                            pipeline.updatedAt()
                    )
                    : pipeline;
            pipelines.removeIf(existing -> existing.id().equals(saved.id()));
            pipelines.add(saved);
            return saved;
        }

        @Override
        public Optional<Pipeline> findById(Long id) {
            return pipelines.stream()
                    .filter(pipeline -> pipeline.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<Pipeline> findByProjectId(Long projectId) {
            return pipelines.stream()
                    .filter(pipeline -> pipeline.projectId().equals(projectId))
                    .toList();
        }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {

        private final Project project;

        private InMemoryProjectRepository(Project project) {
            this.project = project;
        }

        @Override
        public boolean existsByName(String name) {
            return project.name().equals(name);
        }

        @Override
        public Project save(Project project) {
            return project;
        }

        @Override
        public Optional<Project> findById(Long id) {
            return Optional.of(project);
        }

        @Override
        public List<Project> findAll() {
            return List.of(project);
        }
    }

    private static class InMemoryProjectGitRepositoryRepository implements ProjectGitRepositoryRepository {

        private final ProjectGitRepository repository;

        private InMemoryProjectGitRepositoryRepository(ProjectGitRepository repository) {
            this.repository = repository;
        }

        @Override
        public ProjectGitRepository save(ProjectGitRepository repository) {
            return repository;
        }

        @Override
        public Optional<ProjectGitRepository> findByProjectId(Long projectId) {
            return Optional.ofNullable(repository);
        }
    }
}
