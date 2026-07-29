package com.pipeline.platform.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SecretMaskerTest {

    private final SecretMasker secretMasker = new SecretMasker();

    @Test
    void masksSecretWithoutReturningTheOriginalValue() {
        SecretMasker.MaskedSecret maskedSecret = secretMasker.mask("glpat-123456");

        assertThat(maskedSecret.masked()).isEqualTo("********");
        assertThat(maskedSecret.lastFour()).isEqualTo("3456");
    }

    @Test
    void removesKnownSecretsFromLogContent() {
        String sanitized = secretMasker.sanitize(
                "clone with token glpat-123456",
                List.of("glpat-123456")
        );

        assertThat(sanitized).isEqualTo("clone with token ********");
    }
}
