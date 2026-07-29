package com.pipeline.platform.source.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.application.command.VerifyCodeSourceCommand;
import com.pipeline.platform.source.application.model.CodeSourceVerificationView;
import com.pipeline.platform.source.application.model.GitBranchInfo;
import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.model.GitRepositoryInfo;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import com.pipeline.platform.source.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VerifyCodeSourceServiceTest {

    private final Long codeSourceId = 1L;
    private final InMemoryCodeSourceRepository repository = new InMemoryCodeSourceRepository();
    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-29T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void marksCodeSourceAsVerifiedWhenGitLabIsAccessible() {
        CodeSource codeSource = gitLabCodeSource();
        repository.save(codeSource);
        VerifyCodeSourceService useCase = verifyCodeSourceService(new StubGitProviderClient(true));

        CodeSourceVerificationView result = useCase.verify(new VerifyCodeSourceCommand(codeSourceId, null));

        assertThat(result.status()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(result.capabilities().listRepositories()).isTrue();
        assertThat(repository.savedCodeSource.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(repository.savedCodeSource.lastVerifiedAt()).isEqualTo("2026-07-29T05:00Z");
    }

    @Test
    void recordsFailedStatusWhenGitLabIsNotAccessible() {
        CodeSource codeSource = gitLabCodeSource();
        repository.save(codeSource);
        VerifyCodeSourceService useCase = verifyCodeSourceService(new StubGitProviderClient(false));

        assertThatThrownBy(() -> useCase.verify(new VerifyCodeSourceCommand(codeSourceId, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CODE_SOURCE_VERIFICATION_FAILED);

        assertThat(repository.savedCodeSource.verificationStatus()).isEqualTo(VerificationStatus.FAILED);
        assertThat(repository.savedCodeSource.lastVerificationMessage()).isEqualTo("GitLab verification failed");
    }

    private CodeSource gitLabCodeSource() {
        return CodeSource.create(
                codeSourceId,
                "公司 GitLab",
                CodeSourceProvider.GITLAB,
                "https://gitlab.example.com",
                AuthType.ACCESS_TOKEN,
                null,
                "token-for-test",
                clockProvider.now()
        );
    }

    private VerifyCodeSourceService verifyCodeSourceService(GitProviderClient gitProviderClient) {
        VerifyCodeSourceService service = new VerifyCodeSourceService();
        ReflectionTestUtils.setField(service, "codeSourceRepository", repository);
        ReflectionTestUtils.setField(service, "gitProviderClients", List.of(gitProviderClient));
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private static class StubGitProviderClient implements GitProviderClient {

        private final boolean verified;

        private StubGitProviderClient(boolean verified) {
            this.verified = verified;
        }

        @Override
        public boolean supports(CodeSourceProvider provider) {
            return provider == CodeSourceProvider.GITLAB;
        }

        @Override
        public GitProviderVerification verify(CodeSource codeSource, String repositoryPath) {
            if (verified) {
                return GitProviderVerification.verified(
                        "GitLab code source is accessible",
                        GitProviderCapabilities.basicGitLabApi()
                );
            }
            return GitProviderVerification.failed("GitLab verification failed");
        }

        @Override
        public GitRepositoryInfo getRepository(CodeSource codeSource, String repositoryPath) {
            throw new UnsupportedOperationException("Repository lookup is not used by this test");
        }

        @Override
        public List<GitBranchInfo> listBranches(CodeSource codeSource, String repositoryPath) {
            throw new UnsupportedOperationException("Branch lookup is not used by this test");
        }
    }

    private static class InMemoryCodeSourceRepository implements CodeSourceRepository {

        private final List<CodeSource> codeSources = new ArrayList<>();
        private CodeSource savedCodeSource;

        @Override
        public boolean existsByName(String name) {
            return codeSources.stream().anyMatch(codeSource -> codeSource.name().equals(name));
        }

        @Override
        public CodeSource save(CodeSource codeSource) {
            codeSources.removeIf(existing -> existing.id().equals(codeSource.id()));
            codeSources.add(codeSource);
            savedCodeSource = codeSource;
            return codeSource;
        }

        @Override
        public Optional<CodeSource> findById(Long id) {
            return codeSources.stream()
                    .filter(codeSource -> codeSource.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<CodeSource> findAll() {
            return List.copyOf(codeSources);
        }
    }
}
