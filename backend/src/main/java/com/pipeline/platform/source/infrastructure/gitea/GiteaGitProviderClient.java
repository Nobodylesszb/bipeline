package com.pipeline.platform.source.infrastructure.gitea;

import java.util.List;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.source.application.model.GitBranchInfo;
import com.pipeline.platform.source.application.model.GitProviderCapabilities;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.model.GitRepositoryInfo;
import com.pipeline.platform.source.application.model.RepositoryPath;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import io.gitea.ApiException;
import io.gitea.api.RepositoryApi;
import io.gitea.api.UserApi;
import io.gitea.model.Branch;
import io.gitea.model.Repository;
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

    @Override
    public GitRepositoryInfo getRepository(CodeSource codeSource, String repositoryPath) {
        ensureCanUseGiteaApi(codeSource);
        RepositoryPath path = RepositoryPath.parse(repositoryPath);

        try {
            Repository repository = repositoryApi(codeSource).repoGet(path.owner(), path.name());
            return toRepositoryInfo(path, repository);
        } catch (ApiException exception) {
            throw repositoryNotAccessible(exception);
        }
    }

    @Override
    public List<GitBranchInfo> listBranches(CodeSource codeSource, String repositoryPath) {
        ensureCanUseGiteaApi(codeSource);
        RepositoryPath path = RepositoryPath.parse(repositoryPath);

        try {
            return repositoryApi(codeSource)
                    .repoListBranches(path.owner(), path.name(), 1, 100)
                    .stream()
                    .map(this::toBranchInfo)
                    .toList();
        } catch (ApiException exception) {
            throw repositoryNotAccessible(exception);
        }
    }

    private boolean canUseGiteaApi(CodeSource codeSource) {
        return codeSource.authType() == AuthType.ACCESS_TOKEN
                && codeSource.secretPlain() != null
                && !codeSource.secretPlain().isBlank();
    }

    private void ensureCanUseGiteaApi(CodeSource codeSource) {
        if (!canUseGiteaApi(codeSource)) {
            throw new BusinessException(
                    ErrorCode.REPOSITORY_NOT_ACCESSIBLE,
                    "Gitea repository access requires an access token"
            );
        }
    }

    private RepositoryApi repositoryApi(CodeSource codeSource) {
        return new RepositoryApi(giteaApiClient.create(
                codeSource.baseUrl(),
                codeSource.secretPlain()
        ));
    }

    private GitRepositoryInfo toRepositoryInfo(RepositoryPath path, Repository repository) {
        String fullName = repository.getFullName() == null ? path.value() : repository.getFullName();
        return new GitRepositoryInfo(
                path.value(),
                repository.getName(),
                fullName,
                repository.getDefaultBranch(),
                repository.getCloneUrl(),
                repository.getSshUrl()
        );
    }

    private GitBranchInfo toBranchInfo(Branch branch) {
        String commitId = branch.getCommit() == null ? null : branch.getCommit().getId();
        return new GitBranchInfo(branch.getName(), commitId);
    }

    private BusinessException repositoryNotAccessible(ApiException exception) {
        return new BusinessException(
                ErrorCode.REPOSITORY_NOT_ACCESSIBLE,
                messageFor(exception)
        );
    }

    private String messageFor(ApiException exception) {
        if (exception.getCode() > 0) {
            return "Gitea verification failed with HTTP " + exception.getCode();
        }
        return "Gitea verification failed: connection error";
    }
}
