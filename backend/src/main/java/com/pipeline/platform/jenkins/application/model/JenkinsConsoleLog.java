package com.pipeline.platform.jenkins.application.model;

import java.nio.charset.StandardCharsets;

public record JenkinsConsoleLog(
        String text,
        long sizeBytes,
        String externalUrl
) {

    public static JenkinsConsoleLog of(String text, String externalUrl) {
        String safeText = text == null ? "" : text;
        return new JenkinsConsoleLog(
                safeText,
                safeText.getBytes(StandardCharsets.UTF_8).length,
                externalUrl
        );
    }
}
