package com.pipeline.platform.source.infrastructure.gitea;

import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import io.gitea.ApiException;
import io.gitea.api.UserApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GiteaGitProviderClient implements GitProviderClient {

    @Autowired
    private GiteaApiClient giteaApiClient;

    @Override
    public boolean supports(CodeSourceProvider provider) {
        return provider == CodeSourceProvider.GITEA;
    }

    @Override
    public GitProviderVerification verify(CodeSource codeSource, String repositoryPath) {
        if (!canUseGiteaApi(codeSource)) {
            return GitProviderVerification.failed("Gitea verification requires an access token");
        }

        try {
            UserApi userApi = new UserApi(giteaApiClient.create(
                    codeSource.baseUrl(),
                    codeSource.secretPlain()
            ));
            userApi.userGetCurrent();
            userApi.userCurrentListRepos(1, 1);
            return GitProviderVerification.verified(
                    "Gitea code source is accessible",
                    GitProviderCapabilities.basicGiteaApi()
            );
        } catch (ApiException exception) {
            return GitProviderVerification.failed(messageFor(exception));
        }
    }

    private boolean canUseGiteaApi(CodeSource codeSource) {
        return codeSource.authType() == AuthType.ACCESS_TOKEN
                && codeSource.secretPlain() != null
                && !codeSource.secretPlain().isBlank();
    }

    private String messageFor(ApiException exception) {
        if (exception.getCode() > 0) {
            return "Gitea verification failed with HTTP " + exception.getCode();
        }
        return "Gitea verification failed: connection error";
    }
}
