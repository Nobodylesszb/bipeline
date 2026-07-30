package com.pipeline.platform.pipeline.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildLaunch;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildSnapshot;
import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.jenkins.application.model.JenkinsJobDefinition;
import com.pipeline.platform.jenkins.application.model.JenkinsVerification;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.jenkins.domain.JenkinsJob;
import com.pipeline.platform.jenkins.domain.JenkinsJobRepository;
import com.pipeline.platform.jenkins.domain.JenkinsJobSyncStatus;
import com.pipeline.platform.pipeline.application.command.StartPipelineRunCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.pipeline.domain.PipelineRunStatus;
import com.pipeline.platform.pipeline.domain.PipelineStage;
import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.PipelineStep;
import com.pipeline.platform.pipeline.domain.StepType;
import com.pipeline.platform.pipeline.domain.TriggerType;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StartPipelineRunServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-30T03:30:00Z"), ZoneOffset.UTC)
    );

    @Test
    void startsPipelineRunThroughJenkins() {
        InMemoryPipelineRunRepository runRepository = new InMemoryPipelineRunRepository();
        InMemoryJenkinsJobRepository jobRepository = new InMemoryJenkinsJobRepository();
        StubJenkinsClient jenkinsClient = new StubJenkinsClient(7);
        StartPipelineRunService service = service(
                pipelineRepository(activePipeline()),
                runRepository,
                jenkinsRepository(verifiedConnection()),
                jobRepository,
                jenkinsClient
        );

        PipelineRunView result = service.start(new StartPipelineRunCommand(1L, 2L, null, null));

        assertThat(result.status()).isEqualTo(PipelineRunStatus.RUNNING);
        assertThat(result.jenkinsJobName()).isEqualTo("pipeline-1-1-main-ci");
        assertThat(result.jenkinsBuildNumber()).isEqualTo(7);
        assertThat(jenkinsClient.jobDefinition.shellScript()).contains("git clone --branch 'master'");
        assertThat(jenkinsClient.jobDefinition.shellScript()).contains("http://gitea.local/bobo_3776/hotel_link_supply.git");
        assertThat(jenkinsClient.jobDefinition.shellScript()).contains("mvn clean package -DskipTests");
        assertThat(runRepository.findById(result.id())).isPresent();
        JenkinsJob savedJob = jobRepository.findByPipelineIdAndJenkinsConnectionId(1L, 2L).orElseThrow();
        assertThat(savedJob.jobName()).isEqualTo("pipeline-1-1-main-ci");
        assertThat(savedJob.lastSyncStatus()).isEqualTo(JenkinsJobSyncStatus.SYNCED);
    }

    @Test
    void marksPipelineRunAsQueuedWhenJenkinsBuildNumberIsNotReady() {
        StartPipelineRunService service = service(
                pipelineRepository(activePipeline()),
                new InMemoryPipelineRunRepository(),
                jenkinsRepository(verifiedConnection()),
                new InMemoryJenkinsJobRepository(),
                new StubJenkinsClient(null)
        );

        PipelineRunView result = service.start(new StartPipelineRunCommand(1L, 2L, null, null));

        assertThat(result.status()).isEqualTo(PipelineRunStatus.QUEUED);
        assertThat(result.jenkinsQueueUrl()).isEqualTo("http://localhost:8081/queue/item/1/");
        assertThat(result.jenkinsBuildNumber()).isNull();
    }

    @Test
    void rejectsUnverifiedJenkinsConnection() {
        StartPipelineRunService service = service(
                pipelineRepository(activePipeline()),
                new InMemoryPipelineRunRepository(),
                jenkinsRepository(unverifiedConnection()),
                new InMemoryJenkinsJobRepository(),
                new StubJenkinsClient(7)
        );

        assertThatThrownBy(() -> service.start(new StartPipelineRunCommand(1L, 2L, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.JENKINS_CONNECTION_FAILED);
    }

    private StartPipelineRunService service(
            PipelineRepository pipelineRepository,
            PipelineRunRepository runRepository,
            JenkinsConnectionRepository jenkinsConnectionRepository,
            JenkinsJobRepository jenkinsJobRepository,
            JenkinsClient jenkinsClient
    ) {
        PipelineRunJobFactory jobFactory = new PipelineRunJobFactory();
        ReflectionTestUtils.setField(jobFactory, "objectMapper", new ObjectMapper());

        StartPipelineRunService service = new StartPipelineRunService();
        ReflectionTestUtils.setField(service, "pipelineRepository", pipelineRepository);
        ReflectionTestUtils.setField(service, "pipelineRunRepository", runRepository);
        ReflectionTestUtils.setField(service, "projectRepository", projectRepository());
        ReflectionTestUtils.setField(service, "projectGitRepositoryRepository", projectGitRepositoryRepository());
        ReflectionTestUtils.setField(service, "codeSourceRepository", codeSourceRepository());
        ReflectionTestUtils.setField(service, "jenkinsConnectionRepository", jenkinsConnectionRepository);
        ReflectionTestUtils.setField(service, "jenkinsJobRepository", jenkinsJobRepository);
        ReflectionTestUtils.setField(service, "jenkinsClient", jenkinsClient);
        ReflectionTestUtils.setField(service, "jobFactory", jobFactory);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private Pipeline activePipeline() {
        Pipeline pipeline = Pipeline.create(
                1L,
                "main-ci",
                "主分支 CI",
                TriggerType.MANUAL,
                "master",
                List.of(PipelineStage.create(
                        "default",
                        "默认阶段",
                        1,
                        List.of(PipelineStep.create(
                                "test",
                                "运行测试",
                                StepType.SHELL,
                                1,
                                "{\"command\":\"mvn clean package -DskipTests\"}",
                                clockProvider.now()
                        )),
                        clockProvider.now()
                )),
                clockProvider.now()
        );
        Pipeline withId = new Pipeline(
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
        );
        return withId.activate(clockProvider.now());
    }

    private JenkinsConnection verifiedConnection() {
        return connection(VerificationStatus.VERIFIED);
    }

    private JenkinsConnection unverifiedConnection() {
        return connection(VerificationStatus.UNVERIFIED);
    }

    private JenkinsConnection connection(VerificationStatus status) {
        return new JenkinsConnection(
                2L,
                "local-jenkins-bot",
                "http://localhost:8081",
                "pipeline-bot",
                "token-for-test",
                status,
                status == VerificationStatus.VERIFIED ? clockProvider.now() : null,
                null,
                clockProvider.now(),
                clockProvider.now()
        );
    }

    private PipelineRepository pipelineRepository(Pipeline pipeline) {
        return new PipelineRepository() {
            @Override
            public boolean existsByProjectIdAndName(Long projectId, String name) {
                return pipeline.projectId().equals(projectId) && pipeline.name().equals(name);
            }

            @Override
            public Pipeline save(Pipeline pipeline) {
                return pipeline;
            }

            @Override
            public Optional<Pipeline> findById(Long id) {
                return pipeline.id().equals(id) ? Optional.of(pipeline) : Optional.empty();
            }

            @Override
            public List<Pipeline> findByProjectId(Long projectId) {
                return pipeline.projectId().equals(projectId) ? List.of(pipeline) : List.of();
            }
        };
    }

    private JenkinsConnectionRepository jenkinsRepository(JenkinsConnection connection) {
        return new JenkinsConnectionRepository() {
            @Override
            public boolean existsByName(String name) {
                return connection.name().equals(name);
            }

            @Override
            public JenkinsConnection save(JenkinsConnection connection) {
                return connection;
            }

            @Override
            public Optional<JenkinsConnection> findById(Long id) {
                return connection.id().equals(id) ? Optional.of(connection) : Optional.empty();
            }

            @Override
            public List<JenkinsConnection> findAll() {
                return List.of(connection);
            }
        };
    }

    private ProjectRepository projectRepository() {
        Project project = new Project(
                1L,
                "hotel-link",
                "酒店供应链 CI 项目",
                1L,
                com.pipeline.platform.project.domain.ProjectStatus.ACTIVE,
                clockProvider.now(),
                clockProvider.now()
        );
        return new ProjectRepository() {
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
                return project.id().equals(id) ? Optional.of(project) : Optional.empty();
            }

            @Override
            public List<Project> findAll() {
                return List.of(project);
            }
        };
    }

    private ProjectGitRepositoryRepository projectGitRepositoryRepository() {
        ProjectGitRepository repository = ProjectGitRepository.bind(
                1L,
                "bobo_3776/hotel_link_supply",
                "http://localhost:3050/bobo_3776/hotel_link_supply.git",
                "master",
                ".",
                null,
                clockProvider.now()
        );
        return new ProjectGitRepositoryRepository() {
            @Override
            public ProjectGitRepository save(ProjectGitRepository repository) {
                return repository;
            }

            @Override
            public Optional<ProjectGitRepository> findByProjectId(Long projectId) {
                return repository.projectId().equals(projectId) ? Optional.of(repository) : Optional.empty();
            }
        };
    }

    private CodeSourceRepository codeSourceRepository() {
        CodeSource codeSource = CodeSource.create(
                1L,
                "local-gitea",
                CodeSourceProvider.GITEA,
                "http://gitea.local",
                AuthType.ACCESS_TOKEN,
                "bobo_3776",
                "token-for-test",
                clockProvider.now()
        ).withVerification(
                VerificationStatus.VERIFIED,
                clockProvider.now(),
                "Gitea code source is accessible"
        );
        return new CodeSourceRepository() {
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
                return codeSource.id().equals(id) ? Optional.of(codeSource) : Optional.empty();
            }

            @Override
            public List<CodeSource> findAll() {
                return List.of(codeSource);
            }
        };
    }

    private static class InMemoryPipelineRunRepository implements PipelineRunRepository {

        private final List<PipelineRun> runs = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public PipelineRun save(PipelineRun pipelineRun) {
            PipelineRun saved = pipelineRun.id() == null
                    ? new PipelineRun(
                            nextId++,
                            pipelineRun.pipelineId(),
                            pipelineRun.projectId(),
                            pipelineRun.jenkinsConnectionId(),
                            pipelineRun.runNumber(),
                            pipelineRun.status(),
                            pipelineRun.triggerType(),
                            pipelineRun.branch(),
                            pipelineRun.commitSha(),
                            pipelineRun.jenkinsJobName(),
                            pipelineRun.jenkinsQueueUrl(),
                            pipelineRun.jenkinsBuildNumber(),
                            pipelineRun.startedAt(),
                            pipelineRun.finishedAt(),
                            pipelineRun.createdAt(),
                            pipelineRun.updatedAt()
                    )
                    : pipelineRun;
            runs.removeIf(existing -> existing.id().equals(saved.id()));
            runs.add(saved);
            return saved;
        }

        @Override
        public Optional<PipelineRun> findById(Long id) {
            return runs.stream()
                    .filter(run -> run.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<PipelineRun> findLatestByProjectId(Long projectId) {
            return runs.stream()
                    .filter(run -> run.projectId().equals(projectId))
                    .max((left, right) -> left.createdAt().compareTo(right.createdAt()));
        }

        @Override
        public Optional<PipelineRun> findLatestByProjectIdAndPipelineId(Long projectId, Long pipelineId) {
            return runs.stream()
                    .filter(run -> run.projectId().equals(projectId))
                    .filter(run -> run.pipelineId().equals(pipelineId))
                    .max((left, right) -> left.createdAt().compareTo(right.createdAt()));
        }

        @Override
        public List<PipelineRun> findByPipelineId(Long pipelineId) {
            return runs.stream()
                    .filter(run -> run.pipelineId().equals(pipelineId))
                    .toList();
        }

        @Override
        public int nextRunNumber(Long pipelineId) {
            return runs.stream()
                    .filter(run -> run.pipelineId().equals(pipelineId))
                    .mapToInt(PipelineRun::runNumber)
                    .max()
                    .orElse(0) + 1;
        }
    }

    private static class InMemoryJenkinsJobRepository implements JenkinsJobRepository {

        private final List<JenkinsJob> jobs = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public JenkinsJob save(JenkinsJob jenkinsJob) {
            JenkinsJob saved = jenkinsJob.id() == null
                    ? new JenkinsJob(
                            nextId++,
                            jenkinsJob.pipelineId(),
                            jenkinsJob.jenkinsConnectionId(),
                            jenkinsJob.jobName(),
                            jenkinsJob.jobType(),
                            jenkinsJob.configHash(),
                            jenkinsJob.lastSyncedAt(),
                            jenkinsJob.lastSyncStatus(),
                            jenkinsJob.lastSyncMessage(),
                            jenkinsJob.createdAt(),
                            jenkinsJob.updatedAt()
                    )
                    : jenkinsJob;
            jobs.removeIf(existing -> existing.id().equals(saved.id()));
            jobs.add(saved);
            return saved;
        }

        @Override
        public Optional<JenkinsJob> findByPipelineIdAndJenkinsConnectionId(Long pipelineId, Long jenkinsConnectionId) {
            return jobs.stream()
                    .filter(job -> job.pipelineId().equals(pipelineId)
                            && job.jenkinsConnectionId().equals(jenkinsConnectionId))
                    .findFirst();
        }
    }

    private static class StubJenkinsClient implements JenkinsClient {

        private final Integer buildNumber;
        private JenkinsJobDefinition jobDefinition;

        private StubJenkinsClient(Integer buildNumber) {
            this.buildNumber = buildNumber;
        }

        @Override
        public JenkinsVerification verify(JenkinsConnection connection) {
            return JenkinsVerification.verified("Jenkins connection is accessible");
        }

        @Override
        public JenkinsBuildLaunch createOrUpdateFreestyleJobAndBuild(
                JenkinsConnection connection,
                JenkinsJobDefinition jobDefinition
        ) {
            this.jobDefinition = jobDefinition;
            return new JenkinsBuildLaunch("http://localhost:8081/queue/item/1/", buildNumber);
        }

        @Override
        public JenkinsBuildSnapshot getBuild(
                JenkinsConnection connection,
                String jobName,
                Integer buildNumber
        ) {
            return new JenkinsBuildSnapshot(false, "SUCCESS");
        }

        @Override
        public JenkinsConsoleLog getConsoleLog(
                JenkinsConnection connection,
                String jobName,
                Integer buildNumber
        ) {
            throw new UnsupportedOperationException("Not used by this test");
        }

        @Override
        public Integer resolveBuildNumberFromQueue(JenkinsConnection connection, String queueUrl) {
            return buildNumber;
        }
    }
}
