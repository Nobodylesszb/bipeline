package com.pipeline.platform.jenkins.application.model;

public record JenkinsJobDefinition(
        String name,
        String type,
        String configHash,
        String description,
        String shellScript
) {
}
