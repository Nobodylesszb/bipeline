package com.pipeline.platform.jenkins.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.pipeline.platform.jenkins.api.response.JenkinsConnectionResponse;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionView;
import com.pipeline.platform.shared.security.SecretMasker;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JenkinsConnectionResponseMapperTest {

    @Test
    void masksApiTokenInResponse() {
        JenkinsConnectionResponseMapper mapper = new JenkinsConnectionResponseMapper();
        ReflectionTestUtils.setField(mapper, "secretMasker", new SecretMasker());
        JenkinsConnectionView view = new JenkinsConnectionView(
                1L,
                "本地 Jenkins",
                "http://localhost:8080",
                "admin",
                "jenkins-token-1234",
                VerificationStatus.UNVERIFIED,
                null,
                null,
                OffsetDateTime.parse("2026-07-30T10:00:00+08:00"),
                OffsetDateTime.parse("2026-07-30T10:00:00+08:00")
        );

        JenkinsConnectionResponse response = mapper.toResponse(view);

        assertThat(response.apiTokenMasked()).isEqualTo("********");
        assertThat(response.apiTokenLastFour()).isEqualTo("1234");
        assertThat(response.toString()).doesNotContain("jenkins-token-1234");
    }
}
