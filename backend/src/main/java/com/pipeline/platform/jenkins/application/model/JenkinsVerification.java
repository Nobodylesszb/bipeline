package com.pipeline.platform.jenkins.application.model;

public record JenkinsVerification(
        boolean verified,
        String message
) {

    public static JenkinsVerification verified(String message) {
        return new JenkinsVerification(true, message);
    }

    public static JenkinsVerification failed(String message) {
        return new JenkinsVerification(false, message);
    }
}
