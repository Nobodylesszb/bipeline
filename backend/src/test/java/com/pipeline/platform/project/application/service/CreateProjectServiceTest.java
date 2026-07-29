package com.pipeline.platform.project.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.project.application.command.CreateProjectCommand;
import com.pipeline.platform.project.application.model.ProjectView;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.project.domain.ProjectStatus;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CreateProjectServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-29T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createsActiveProjectWhenCodeSourceIsVerified() {
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCodeSourceRepository codeSourceRepository = new InMemoryCodeSourceRepository();
        codeSourceRepository.save(verifiedCodeSource());
        CreateProjectService service = createProjectService(projectRepository, codeSourceRepository);

        ProjectView result = service.create(new CreateProjectCommand(
                "电商项目",
                "订单服务 CI",
                1L
        ));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("电商项目");
        assertThat(result.codeSourceId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void rejectsProjectWhenCodeSourceIsNotVerified() {
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryCodeSourceRepository codeSourceRepository = new InMemoryCodeSourceRepository();
        codeSourceRepository.save(unverifiedCodeSource());
        CreateProjectService service = createProjectService(projectRepository, codeSourceRepository);

        assertThatThrownBy(() -> service.create(new CreateProjectCommand(
                "电商项目",
                null,
                1L
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CODE_SOURCE_VERIFICATION_FAILED);
    }

    private CreateProjectService createProjectService(
            ProjectRepository projectRepository,
            CodeSourceRepository codeSourceRepository
    ) {
        CodeSourceGuard guard = new CodeSourceGuard();
        ReflectionTestUtils.setField(guard, "codeSourceRepository", codeSourceRepository);

        CreateProjectService service = new CreateProjectService();
        ReflectionTestUtils.setField(service, "projectRepository", projectRepository);
        ReflectionTestUtils.setField(service, "codeSourceGuard", guard);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private CodeSource verifiedCodeSource() {
        return unverifiedCodeSource().withVerification(
                com.pipeline.platform.source.domain.VerificationStatus.VERIFIED,
                clockProvider.now(),
                "Gitea code source is accessible"
        );
    }

    private CodeSource unverifiedCodeSource() {
        return CodeSource.create(
                1L,
                "本地 Gitea",
                CodeSourceProvider.GITEA,
                "http://localhost:3000",
                AuthType.ACCESS_TOKEN,
                "bo",
                "token-for-test",
                clockProvider.now()
        );
    }

    private static class InMemoryProjectRepository implements ProjectRepository {

        private final List<Project> projects = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public boolean existsByName(String name) {
            return projects.stream().anyMatch(project -> project.name().equals(name));
        }

        @Override
        public Project save(Project project) {
            Project saved = project.id() == null
                    ? new Project(
                            nextId++,
                            project.name(),
                            project.description(),
                            project.codeSourceId(),
                            project.status(),
                            project.createdAt(),
                            project.updatedAt()
                    )
                    : project;
            projects.removeIf(existing -> existing.id().equals(saved.id()));
            projects.add(saved);
            return saved;
        }

        @Override
        public Optional<Project> findById(Long id) {
            return projects.stream()
                    .filter(project -> project.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return List.copyOf(projects);
        }
    }

    private static class InMemoryCodeSourceRepository implements CodeSourceRepository {

        private final List<CodeSource> codeSources = new ArrayList<>();

        @Override
        public boolean existsByName(String name) {
            return codeSources.stream().anyMatch(codeSource -> codeSource.name().equals(name));
        }

        @Override
        public CodeSource save(CodeSource codeSource) {
            codeSources.removeIf(existing -> existing.id().equals(codeSource.id()));
            codeSources.add(codeSource);
            return codeSource;
        }

        @Override
        public Optional<CodeSource> findById(Long id) {
            return codeSources.stream()
                    .filter(codeSource -> codeSource.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<CodeSource> findAll() {
            return List.copyOf(codeSources);
        }
    }
}
