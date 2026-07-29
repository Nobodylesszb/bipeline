package com.pipeline.platform.source.application.model;

public record GitProviderVerification(
        boolean verified,
        String message,
        GitProviderCapabilities capabilities
) {

    public static GitProviderVerification verified(String message, GitProviderCapabilities capabilities) {
        return new GitProviderVerification(true, message, capabilities);
    }

    public static GitProviderVerification failed(String message) {
        return new GitProviderVerification(false, message, GitProviderCapabilities.none());
    }
}
