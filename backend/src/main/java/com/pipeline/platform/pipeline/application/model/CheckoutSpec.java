package com.pipeline.platform.pipeline.application.model;

public record CheckoutSpec(
        String remoteUrl,
        String branch,
        String contextDirectory,
        String username,
        String secret
) {
}
