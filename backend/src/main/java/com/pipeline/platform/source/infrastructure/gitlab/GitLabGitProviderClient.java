package com.pipeline.platform.source.infrastructure.gitlab;

import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GitLabGitProviderClient implements GitProviderClient {

    @Autowired
    private GitLabApiFactory gitLabApiFactory;

    @Override
    public boolean supports(CodeSourceProvider provider) {
        return provider == CodeSourceProvider.GITLAB;
    }

    @Override
    public GitProviderVerification verify(CodeSource codeSource, String repositoryPath) {
        if (!canUseGitLabApi(codeSource)) {
            return GitProviderVerification.failed("GitLab verification requires an access token");
        }

        try (GitLabApi gitLabApi = gitLabApiFactory.create(codeSource.baseUrl(), codeSource.secretPlain())) {
            gitLabApi.getUserApi().getCurrentUser();
            gitLabApi.getProjectApi().getMemberProjects(1, 1);

            return GitProviderVerification.verified(
                    "GitLab code source is accessible",
                    GitProviderCapabilities.basicGitLabApi()
            );
        } catch (GitLabApiException exception) {
            return GitProviderVerification.failed(messageFor(exception));
        }
    }

    private boolean canUseGitLabApi(CodeSource codeSource) {
        return codeSource.authType() == AuthType.ACCESS_TOKEN
                && codeSource.secretPlain() != null
                && !codeSource.secretPlain().isBlank();
    }

    private String messageFor(GitLabApiException exception) {
        if (exception.getHttpStatus() > 0) {
            return "GitLab verification failed with HTTP " + exception.getHttpStatus();
        }
        return "GitLab verification failed: connection error";
    }
}
