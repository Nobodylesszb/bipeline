package com.pipeline.platform.pipeline.application.service;

import java.util.List;

import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.security.SecretMasker;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PipelineRunLogFetcher {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private JenkinsConnectionRepository jenkinsConnectionRepository;

    @Autowired
    private JenkinsClient jenkinsClient;

    @Autowired
    private SecretMasker secretMasker;

    @Autowired
    private ClockProvider clockProvider;

    public JenkinsConsoleLog fetch(PipelineRun pipelineRun) {
        JenkinsConnection connection = jenkinsConnectionRepository.findById(pipelineRun.jenkinsConnectionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Jenkins connection not found"
                ));
        PipelineRun runWithBuildNumber = ensureBuildNumber(connection, pipelineRun);
        JenkinsConsoleLog rawLog = jenkinsClient.getConsoleLog(
                connection,
                runWithBuildNumber.jenkinsJobName(),
                runWithBuildNumber.jenkinsBuildNumber()
        );
        return new JenkinsConsoleLog(
                secretMasker.sanitize(rawLog.text(), List.of(connection.apiTokenPlain())),
                rawLog.sizeBytes(),
                rawLog.externalUrl()
        );
    }

    private PipelineRun ensureBuildNumber(JenkinsConnection connection, PipelineRun pipelineRun) {
        if (pipelineRun.jenkinsBuildNumber() != null) {
            return pipelineRun;
        }
        Integer buildNumber = jenkinsClient.resolveBuildNumberFromQueue(
                connection,
                pipelineRun.jenkinsQueueUrl()
        );
        if (buildNumber == null) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ENGINE_UNAVAILABLE,
                    "Jenkins build number is not available yet"
            );
        }
        return pipelineRunRepository.save(pipelineRun.markRunning(
                pipelineRun.jenkinsQueueUrl(),
                buildNumber,
                clockProvider.now()
        ));
    }
}
