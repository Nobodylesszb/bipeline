package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.jenkins.application.model.JenkinsBuildSnapshot;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.pipeline.application.command.PipelineRunDetailCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunView;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.pipeline.domain.PipelineRunStatus;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SyncPipelineRunStatusService {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Autowired
    private JenkinsClient jenkinsClient;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public PipelineRunView sync(PipelineRunDetailCommand command) {
        PipelineRun pipelineRun = requirePipelineRun(command.pipelineRunId());
        if (pipelineRun.isTerminal()) {
            return PipelineRunView.from(pipelineRun);
        }
        JenkinsConnection connection = requireJenkinsConnection(pipelineRun.jenkinsConnectionId());
        pipelineRun = resolveBuildNumber(connection, pipelineRun);
        JenkinsBuildSnapshot snapshot = jenkinsClient.getBuild(
                connection,
                pipelineRun.jenkinsJobName(),
                pipelineRun.jenkinsBuildNumber()
        );
        if (snapshot.building() || snapshot.result() == null) {
            pipelineRun = markRunningIfBuildStarted(pipelineRun);
            return PipelineRunView.from(pipelineRun);
        }
        PipelineRunStatus status = toStatus(snapshot.result());
        return PipelineRunView.from(pipelineRunRepository.save(pipelineRun.finish(status, clockProvider.now())));
    }

    private PipelineRun requirePipelineRun(Long pipelineRunId) {
        return pipelineRunRepository.findById(pipelineRunId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline run not found"
                ));
    }

    private JenkinsConnection requireJenkinsConnection(Long connectionId) {
        return jenkinsConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Jenkins connection not found"
                ));
    }

    private PipelineRunStatus toStatus(String jenkinsResult) {
        return switch (jenkinsResult) {
            case "SUCCESS" -> PipelineRunStatus.SUCCESS;
            case "ABORTED" -> PipelineRunStatus.CANCELED;
            default -> PipelineRunStatus.FAILED;
        };
    }

    private PipelineRun resolveBuildNumber(JenkinsConnection connection, PipelineRun pipelineRun) {
        if (pipelineRun.jenkinsBuildNumber() != null) {
            return pipelineRun;
        }
        Integer buildNumber = jenkinsClient.resolveBuildNumberFromQueue(connection, pipelineRun.jenkinsQueueUrl());
        if (buildNumber == null) {
            return pipelineRun;
        }
        return pipelineRunRepository.save(pipelineRun.markRunning(
                pipelineRun.jenkinsQueueUrl(),
                buildNumber,
                clockProvider.now()
        ));
    }

    private PipelineRun markRunningIfBuildStarted(PipelineRun pipelineRun) {
        if (pipelineRun.status() == PipelineRunStatus.RUNNING || pipelineRun.jenkinsBuildNumber() == null) {
            return pipelineRun;
        }
        return pipelineRunRepository.save(pipelineRun.markRunning(
                pipelineRun.jenkinsQueueUrl(),
                pipelineRun.jenkinsBuildNumber(),
                clockProvider.now()
        ));
    }
}
