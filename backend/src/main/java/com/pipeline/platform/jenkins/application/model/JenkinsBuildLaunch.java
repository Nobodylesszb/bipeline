package com.pipeline.platform.jenkins.application.model;

public record JenkinsBuildLaunch(
        String queueUrl,
        Integer buildNumber
) {
}
