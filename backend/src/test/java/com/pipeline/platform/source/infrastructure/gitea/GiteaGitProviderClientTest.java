package com.pipeline.platform.source.infrastructure.gitea;

import static org.assertj.core.api.Assertions.assertThat;

import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import org.junit.jupiter.api.Test;

class GiteaGitProviderClientTest {

    @Test
    void supportsGiteaProvider() {
        GiteaGitProviderClient client = new GiteaGitProviderClient();

        assertThat(client.supports(CodeSourceProvider.GITEA)).isTrue();
        assertThat(client.supports(CodeSourceProvider.GITLAB)).isFalse();
    }

    @Test
    void rejectsVerificationWhenAuthTypeCannotCallGiteaApi() {
        GiteaGitProviderClient client = new GiteaGitProviderClient();
        CodeSource codeSource = CodeSource.create(
                1L,
                "本地 Gitea",
                CodeSourceProvider.GITEA,
                "http://localhost:3000",
                AuthType.SSH_KEY,
                null,
                "ssh-key-for-test",
                java.time.OffsetDateTime.parse("2026-07-29T15:40:00+08:00")
        );

        GitProviderVerification verification = client.verify(codeSource, null);

        assertThat(verification.verified()).isFalse();
        assertThat(verification.message()).isEqualTo("Gitea verification requires an access token");
    }
}
