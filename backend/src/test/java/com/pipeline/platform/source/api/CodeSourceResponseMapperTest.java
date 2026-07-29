package com.pipeline.platform.source.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.pipeline.platform.shared.security.SecretMasker;
import com.pipeline.platform.source.application.CodeSourceView;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;

class CodeSourceResponseMapperTest {

    private final CodeSourceResponseMapper mapper = new CodeSourceResponseMapper(new SecretMasker());

    @Test
    void masksSecretInResponse() {
        CodeSourceView view = new CodeSourceView(
                UUID.randomUUID(),
                "公司 GitLab",
                CodeSourceProvider.GITLAB,
                "https://gitlab.example.com",
                AuthType.DEPLOY_TOKEN,
                "ci-reader",
                "glpat-123456",
                VerificationStatus.UNVERIFIED,
                null,
                null,
                OffsetDateTime.parse("2026-07-29T12:00:00+08:00"),
                OffsetDateTime.parse("2026-07-29T12:00:00+08:00")
        );

        CodeSourceResponse response = mapper.toResponse(view);

        assertThat(response.secretMasked()).isEqualTo("********");
        assertThat(response.secretLastFour()).isEqualTo("3456");
        assertThat(response.toString()).doesNotContain("glpat-123456");
    }
}
