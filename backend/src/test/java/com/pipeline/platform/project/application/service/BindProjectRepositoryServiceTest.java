package com.pipeline.platform.project.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.project.application.command.BindProjectRepositoryCommand;
import com.pipeline.platform.project.application.model.ProjectGitRepositoryView;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.application.model.GitBranchInfo;
import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.model.GitRepositoryInfo;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BindProjectRepositoryServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-29T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void bindsRepositoryWhenDefaultBranchExists() {
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        projectRepository.save(project());
        InMemoryProjectGitRepositoryRepository repositoryRepository = new InMemoryProjectGitRepositoryRepository();
        BindProjectRepositoryService service = bindService(
                projectRepository,
                repositoryRepository,
                new InMemoryCodeSourceRepository(verifiedCodeSource()),
                new StubGitProviderClient()
        );

        ProjectGitRepositoryView result = service.bind(new BindProjectRepositoryCommand(
                1L,
                1L,
                "bobo_3776/hotel_link_supply",
                "master",
                "."
        ));

        assertThat(result.projectId()).isEqualTo(1L);
        assertThat(result.remotePath()).isEqualTo("bobo_3776/hotel_link_supply");
        assertThat(result.defaultBranch()).isEqualTo("master");
        assertThat(result.lastResolvedRevision()).isEqualTo("commit-master");
        assertThat(repositoryRepository.findByProjectId(1L)).isPresent();
    }

    @Test
    void rejectsBindWhenDefaultBranchDoesNotExist() {
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        projectRepository.save(project());
        BindProjectRepositoryService service = bindService(
                projectRepository,
                new InMemoryProjectGitRepositoryRepository(),
                new InMemoryCodeSourceRepository(verifiedCodeSource()),
                new StubGitProviderClient()
        );

        assertThatThrownBy(() -> service.bind(new BindProjectRepositoryCommand(
                1L,
                1L,
                "bobo_3776/hotel_link_supply",
                "release",
                "."
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPOSITORY_NOT_ACCESSIBLE);
    }

    private BindProjectRepositoryService bindService(
            ProjectRepository projectRepository,
            ProjectGitRepositoryRepository projectGitRepositoryRepository,
            CodeSourceRepository codeSourceRepository,
            GitProviderClient gitProviderClient
    ) {
        CodeSourceGuard guard = new CodeSourceGuard();
        ReflectionTestUtils.setField(guard, "codeSourceRepository", codeSourceRepository);

        GitProviderResolver resolver = new GitProviderResolver();
        ReflectionTestUtils.setField(resolver, "gitProviderClients", List.of(gitProviderClient));

        BindProjectRepositoryService service = new BindProjectRepositoryService();
        ReflectionTestUtils.setField(service, "projectRepository", projectRepository);
        ReflectionTestUtils.setField(service, "projectGitRepositoryRepository", projectGitRepositoryRepository);
        ReflectionTestUtils.setField(service, "codeSourceGuard", guard);
        ReflectionTestUtils.setField(service, "gitProviderResolver", resolver);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private Project project() {
        return Project.create(
                "电商项目",
                "订单服务 CI",
                1L,
                clockProvider.now()
        );
    }

    private CodeSource verifiedCodeSource() {
        return CodeSource.create(
                        1L,
                        "本地 Gitea",
                        CodeSourceProvider.GITEA,
                        "http://localhost:3000",
                        AuthType.ACCESS_TOKEN,
                        "bo",
                        "token-for-test",
                        clockProvider.now()
                )
                .withVerification(
                        VerificationStatus.VERIFIED,
                        clockProvider.now(),
                        "Gitea code source is accessible"
                );
    }

    private static class StubGitProviderClient implements GitProviderClient {

        @Override
        public boolean supports(CodeSourceProvider provider) {
            return provider == CodeSourceProvider.GITEA;
        }

        @Override
        public GitProviderVerification verify(CodeSource codeSource, String repositoryPath) {
            return GitProviderVerification.verified(
                    "Gitea code source is accessible",
                    GitProviderCapabilities.basicGiteaApi()
            );
        }

        @Override
        public GitRepositoryInfo getRepository(CodeSource codeSource, String repositoryPath) {
            return new GitRepositoryInfo(
                    repositoryPath,
                    "hotel_link_supply",
                    repositoryPath,
                    "master",
                    "http://localhost:3000/bobo_3776/hotel_link_supply.git",
                    "ssh://git@localhost:2222/bobo_3776/hotel_link_supply.git"
            );
        }

        @Override
        public List<GitBranchInfo> listBranches(CodeSource codeSource, String repositoryPath) {
            return List.of(new GitBranchInfo("master", "commit-master"));
        }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {

        private final List<Project> projects = new ArrayList<>();

        @Override
        public boolean existsByName(String name) {
            return projects.stream().anyMatch(project -> project.name().equals(name));
        }

        @Override
        public Project save(Project project) {
            Project saved = project.id() == null
                    ? new Project(
                            1L,
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

    private static class InMemoryProjectGitRepositoryRepository implements ProjectGitRepositoryRepository {

        private ProjectGitRepository repository;

        @Override
        public ProjectGitRepository save(ProjectGitRepository repository) {
            ProjectGitRepository saved = repository.id() == null
                    ? new ProjectGitRepository(
                            1L,
                            repository.projectId(),
                            repository.remotePath(),
                            repository.remoteUrl(),
                            repository.defaultBranch(),
                            repository.contextDirectory(),
                            repository.lastResolvedRevision(),
                            repository.lastFetchedAt(),
                            repository.createdAt(),
                            repository.updatedAt()
                    )
                    : repository;
            this.repository = saved;
            return saved;
        }

        @Override
        public Optional<ProjectGitRepository> findByProjectId(Long projectId) {
            if (repository == null || !repository.projectId().equals(projectId)) {
                return Optional.empty();
            }
            return Optional.of(repository);
        }
    }

    private static class InMemoryCodeSourceRepository implements CodeSourceRepository {

        private final CodeSource codeSource;

        private InMemoryCodeSourceRepository(CodeSource codeSource) {
            this.codeSource = codeSource;
        }

        @Override
        public boolean existsByName(String name) {
            return codeSource.name().equals(name);
        }

        @Override
        public CodeSource save(CodeSource codeSource) {
            return codeSource;
        }

        @Override
        public Optional<CodeSource> findById(Long id) {
            if (codeSource.id().equals(id)) {
                return Optional.of(codeSource);
            }
            return Optional.empty();
        }

        @Override
        public List<CodeSource> findAll() {
            return List.of(codeSource);
        }
    }
}
