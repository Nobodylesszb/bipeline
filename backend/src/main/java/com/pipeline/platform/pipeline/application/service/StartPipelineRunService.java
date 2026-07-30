package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.jenkins.application.model.JenkinsBuildLaunch;
import com.pipeline.platform.jenkins.application.model.JenkinsJobDefinition;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.jenkins.domain.JenkinsJob;
import com.pipeline.platform.jenkins.domain.JenkinsJobRepository;
import com.pipeline.platform.jenkins.domain.JenkinsJobType;
import com.pipeline.platform.pipeline.application.command.StartPipelineRunCommand;
import com.pipeline.platform.pipeline.application.model.CheckoutSpec;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.project.domain.Project;
import com.pipeline.platform.project.domain.ProjectGitRepository;
import com.pipeline.platform.project.domain.ProjectGitRepositoryRepository;
import com.pipeline.platform.project.domain.ProjectRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartPipelineRunService {

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGitRepositoryRepository projectGitRepositoryRepository;

    @Autowired
    private CodeSourceRepository codeSourceRepository;

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Autowired
    private JenkinsJobRepository jenkinsJobRepository;

    @Autowired
    private JenkinsClient jenkinsClient;

    @Autowired
    private PipelineRunJobFactory jobFactory;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public PipelineRunView start(StartPipelineRunCommand command) {
        Pipeline pipeline = requirePipeline(command.pipelineId());
        JenkinsConnection connection = requireVerifiedJenkinsConnection(command.jenkinsConnectionId());
        CheckoutSpec checkoutSpec = checkoutSpec(pipeline, command.branch());
        JenkinsJobDefinition jobDefinition = jobFactory.toFreestyleJob(pipeline, checkoutSpec);
        JenkinsJob jenkinsJob = savePendingJob(pipeline, connection, jobDefinition);
        PipelineRun pipelineRun = PipelineRun.start(
                pipeline,
                connection.id(),
                pipelineRunRepository.nextRunNumber(pipeline.id()),
                command.branch(),
                command.commitSha(),
                jenkinsJob.jobName(),
                clockProvider.now()
        );
        PipelineRun savedRun = pipelineRunRepository.save(pipelineRun);
        JenkinsBuildLaunch launch = jenkinsClient.createOrUpdateFreestyleJobAndBuild(connection, jobDefinition);
        jenkinsJobRepository.save(jenkinsJob.markSynced("Jenkins job synced and build triggered", clockProvider.now()));
        return PipelineRunView.from(pipelineRunRepository.save(startedRun(savedRun, launch)));
    }

    private Pipeline requirePipeline(Long pipelineId) {
        return pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline not found"
                ));
    }

    private JenkinsConnection requireVerifiedJenkinsConnection(Long connectionId) {
        JenkinsConnection connection = jenkinsConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Jenkins connection not found"
                ));
        if (connection.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(
                    ErrorCode.JENKINS_CONNECTION_FAILED,
                    "Jenkins connection must be verified before running pipeline"
            );
        }
        return connection;
    }

    private String branch(String requestedBranch, String pipelineBranch) {
        if (requestedBranch == null || requestedBranch.isBlank()) {
            return pipelineBranch;
        }
        return requestedBranch.trim();
    }

    private CheckoutSpec checkoutSpec(Pipeline pipeline, String requestedBranch) {
        Project project = projectRepository.findById(pipeline.projectId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Project not found"
                ));
        if (!project.active()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Project is not active"
            );
        }
        ProjectGitRepository repository = projectGitRepositoryRepository.findByProjectId(project.id())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "Project repository is not bound"
                ));
        CodeSource codeSource = codeSourceRepository.findById(project.codeSourceId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Code source not found"
                ));
        if (codeSource.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(
                    ErrorCode.CODE_SOURCE_VERIFICATION_FAILED,
                    "Code source must be verified before running pipeline"
            );
        }
        return new CheckoutSpec(
                checkoutRemoteUrl(codeSource, repository),
                branch(requestedBranch, pipeline.branchName()),
                repository.contextDirectory(),
                codeSource.username(),
                codeSource.secretPlain()
        );
    }

    private JenkinsJob savePendingJob(
            Pipeline pipeline,
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition
    ) {
        return jenkinsJobRepository.findByPipelineIdAndJenkinsConnectionId(pipeline.id(), connection.id())
                .map(existing -> jenkinsJobRepository.save(existing.withDefinition(
                        jobDefinition.name(),
                        JenkinsJobType.valueOf(jobDefinition.type()),
                        jobDefinition.configHash(),
                        clockProvider.now()
                )))
                .orElseGet(() -> jenkinsJobRepository.save(JenkinsJob.create(
                        pipeline.id(),
                        connection.id(),
                        jobDefinition.name(),
                        JenkinsJobType.valueOf(jobDefinition.type()),
                        jobDefinition.configHash(),
                        clockProvider.now()
                )));
    }

    private String checkoutRemoteUrl(CodeSource codeSource, ProjectGitRepository repository) {
        if (repository.remotePath() == null || repository.remotePath().isBlank()) {
            return repository.remoteUrl();
        }
        String suffix = repository.remotePath().endsWith(".git")
                ? repository.remotePath()
                : repository.remotePath() + ".git";
        return codeSource.baseUrl() + "/" + suffix;
    }

    private PipelineRun startedRun(PipelineRun savedRun, JenkinsBuildLaunch launch) {
        if (launch.buildNumber() == null) {
            return savedRun.markQueued(launch.queueUrl(), clockProvider.now());
        }
        return savedRun.markRunning(launch.queueUrl(), launch.buildNumber(), clockProvider.now());
    }
}
