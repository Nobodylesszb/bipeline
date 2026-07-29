package com.pipeline.platform.source.infrastructure.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.springframework.stereotype.Component;

@Component
public class GitLabApiFactory {

    public GitLabApi create(String baseUrl, String accessToken) {
        GitLabApi gitLabApi = new GitLabApi(baseUrl, accessToken);
        gitLabApi.setRequestTimeout(5_000, 10_000);
        return gitLabApi;
    }
}
