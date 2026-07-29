package com.pipeline.platform.shared.security;

import org.springframework.stereotype.Component;

@Component
public class SecretMasker {

    private static final String MASK = "********";

    public MaskedSecret mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return new MaskedSecret(MASK, "");
        }
        String trimmed = secret.trim();
        String lastFour = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return new MaskedSecret(MASK, lastFour);
    }

    public String sanitize(String content, Iterable<String> secrets) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String sanitized = content;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                sanitized = sanitized.replace(secret, MASK);
            }
        }
        return sanitized;
    }

    public record MaskedSecret(String masked, String lastFour) {
    }
}
