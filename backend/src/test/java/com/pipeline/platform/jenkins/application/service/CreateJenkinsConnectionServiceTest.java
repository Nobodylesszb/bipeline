package com.pipeline.platform.jenkins.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.jenkins.application.command.CreateJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.model.JenkinsConnectionView;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.jenkins.domain.JenkinsConnectionRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CreateJenkinsConnectionServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-30T02:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createsUnverifiedConnectionAndNormalizesBaseUrl() {
        InMemoryJenkinsConnectionRepository repository = new InMemoryJenkinsConnectionRepository();
        CreateJenkinsConnectionService service = service(repository);

        JenkinsConnectionView result = service.create(new CreateJenkinsConnectionCommand(
                "本地 Jenkins",
                "http://localhost:8080/",
                "admin",
                "token-for-test"
        ));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(result.verificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(result.apiTokenPlain()).isEqualTo("token-for-test");
    }

    @Test
    void rejectsDuplicateConnectionName() {
        InMemoryJenkinsConnectionRepository repository = new InMemoryJenkinsConnectionRepository();
        CreateJenkinsConnectionService service = service(repository);
        service.create(new CreateJenkinsConnectionCommand(
                "本地 Jenkins",
                "http://localhost:8080",
                "admin",
                "token-for-test"
        ));

        assertThatThrownBy(() -> service.create(new CreateJenkinsConnectionCommand(
                "本地 Jenkins",
                "http://localhost:8081",
                "admin",
                "another-token"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    private CreateJenkinsConnectionService service(JenkinsConnectionRepository repository) {
        CreateJenkinsConnectionService service = new CreateJenkinsConnectionService();
        ReflectionTestUtils.setField(service, "jenkinsConnectionRepository", repository);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    static class InMemoryJenkinsConnectionRepository implements JenkinsConnectionRepository {

        private final List<JenkinsConnection> connections = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public boolean existsByName(String name) {
            return connections.stream().anyMatch(connection -> connection.name().equals(name));
        }

        @Override
        public JenkinsConnection save(JenkinsConnection connection) {
            JenkinsConnection saved = connection.id() == null
                    ? new JenkinsConnection(
                            nextId++,
                            connection.name(),
                            connection.baseUrl(),
                            connection.username(),
                            connection.apiTokenPlain(),
                            connection.verificationStatus(),
                            connection.lastVerifiedAt(),
                            connection.lastVerificationMessage(),
                            connection.createdAt(),
                            connection.updatedAt()
                    )
                    : connection;
            connections.removeIf(existing -> existing.id().equals(saved.id()));
            connections.add(saved);
            return saved;
        }

        @Override
        public Optional<JenkinsConnection> findById(Long id) {
            return connections.stream()
                    .filter(connection -> connection.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<JenkinsConnection> findAll() {
            return List.copyOf(connections);
        }
    }
}
