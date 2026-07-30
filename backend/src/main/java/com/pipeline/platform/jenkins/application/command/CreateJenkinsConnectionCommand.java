package com.pipeline.platform.jenkins.application.command;

public record CreateJenkinsConnectionCommand(
        String name,
        String baseUrl,
        String username,
        String apiToken
) {
}
