package com.pipeline.platform.jenkins.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.pipeline.platform.jenkins.application.command.VerifyJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildLaunch;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildSnapshot;
import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionVerificationView;
import com.pipeline.platform.jenkins.application.model.JenkinsJobDefinition;
import com.pipeline.platform.jenkins.application.model.JenkinsVerification;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VerifyJenkinsConnectionServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-30T02:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void marksConnectionAsVerifiedWhenJenkinsIsAccessible() {
        var repository = new CreateJenkinsConnectionServiceTest.InMemoryJenkinsConnectionRepository();
        repository.save(connection());
        VerifyJenkinsConnectionService service = service(repository, JenkinsVerification.verified(
                "Jenkins connection is accessible"
        ));

        JenkinsConnectionVerificationView result = service.verify(new VerifyJenkinsConnectionCommand(1L));

        assertThat(result.status()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(result.message()).isEqualTo("Jenkins connection is accessible");
        assertThat(repository.findById(1L).orElseThrow().verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void recordsFailedStatusWhenJenkinsIsNotAccessible() {
        var repository = new CreateJenkinsConnectionServiceTest.InMemoryJenkinsConnectionRepository();
        repository.save(connection());
        VerifyJenkinsConnectionService service = service(repository, JenkinsVerification.failed(
                "Jenkins verification failed with HTTP 401"
        ));

        assertThatThrownBy(() -> service.verify(new VerifyJenkinsConnectionCommand(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.JENKINS_CONNECTION_FAILED);
        assertThat(repository.findById(1L).orElseThrow().verificationStatus()).isEqualTo(VerificationStatus.FAILED);
    }

    private VerifyJenkinsConnectionService service(
            CreateJenkinsConnectionServiceTest.InMemoryJenkinsConnectionRepository repository,
            JenkinsVerification verification
    ) {
        VerifyJenkinsConnectionService service = new VerifyJenkinsConnectionService();
        ReflectionTestUtils.setField(service, "jenkinsConnectionRepository", repository);
        ReflectionTestUtils.setField(service, "jenkinsClient", new StubJenkinsClient(verification));
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private JenkinsConnection connection() {
        return JenkinsConnection.create(
                "本地 Jenkins",
                "http://localhost:8080",
                "admin",
                "token-for-test",
                clockProvider.now()
        );
    }

    private record StubJenkinsClient(JenkinsVerification verification) implements JenkinsClient {

        @Override
        public JenkinsVerification verify(JenkinsConnection connection) {
            return verification;
        }

        @Override
        public JenkinsBuildLaunch createOrUpdateFreestyleJobAndBuild(
                JenkinsConnection connection,
                JenkinsJobDefinition jobDefinition
        ) {
            throw new UnsupportedOperationException("Not used by this test");
        }

        @Override
        public JenkinsBuildSnapshot getBuild(
                JenkinsConnection connection,
                String jobName,
                Integer buildNumber
        ) {
            throw new UnsupportedOperationException("Not used by this test");
        }

        @Override
        public JenkinsConsoleLog getConsoleLog(
                JenkinsConnection connection,
                String jobName,
                Integer buildNumber
        ) {
            throw new UnsupportedOperationException("Not used by this test");
        }

        @Override
        public Integer resolveBuildNumberFromQueue(JenkinsConnection connection, String queueUrl) {
            throw new UnsupportedOperationException("Not used by this test");
        }
    }
}
