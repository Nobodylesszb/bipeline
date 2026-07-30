package com.pipeline.platform.jenkins.application.port;

import com.pipeline.platform.jenkins.application.model.JenkinsVerification;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildLaunch;
import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildSnapshot;
import com.pipeline.platform.jenkins.application.model.JenkinsJobDefinition;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;

public interface JenkinsClient {

    JenkinsVerification verify(JenkinsConnection connection);

    JenkinsBuildLaunch createOrUpdateFreestyleJobAndBuild(
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition
    );

    JenkinsBuildSnapshot getBuild(
            JenkinsConnection connection,
            String jobName,
            Integer buildNumber
    );

    JenkinsConsoleLog getConsoleLog(
            JenkinsConnection connection,
            String jobName,
            Integer buildNumber
    );

    Integer resolveBuildNumberFromQueue(
            JenkinsConnection connection,
            String queueUrl
    );
}
