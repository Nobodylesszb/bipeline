package com.pipeline.platform.jenkins.application.model;

public record JenkinsBuildSnapshot(
        boolean building,
        String result
) {
}
