package com.pipeline.platform.source.infrastructure.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GitLabGitProviderClientTest {

    @Test
    void rejectsVerificationWhenAuthTypeCannotCallGitLabApi() {
        GitLabGitProviderClient client = new GitLabGitProviderClient();
        ReflectionTestUtils.setField(client, "gitLabApiFactory", new GitLabApiFactory());
        CodeSource codeSource = CodeSource.create(
                1L,
                "公司 GitLab",
                CodeSourceProvider.GITLAB,
                "https://gitlab.example.com",
                AuthType.SSH_KEY,
                null,
                "ssh-key-for-test",
                java.time.OffsetDateTime.parse("2026-07-29T13:00:00+08:00")
        );

        GitProviderVerification verification = client.verify(codeSource, null);

        assertThat(verification.verified()).isFalse();
        assertThat(verification.message()).isEqualTo("GitLab verification requires an access token");
    }
}
