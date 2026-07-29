package com.pipeline.platform.source.infrastructure.gitea;

import io.gitea.ApiClient;
import io.gitea.auth.ApiKeyAuth;
import org.springframework.stereotype.Component;

@Component
public class GiteaApiClient {

    public ApiClient create(String baseUrl, String accessToken) {
        ApiClient client = new ApiClient();
        client.setBasePath(apiBasePath(baseUrl));
        client.setConnectTimeout(5_000);
        client.setReadTimeout(10_000);

        ApiKeyAuth tokenAuth = (ApiKeyAuth) client.getAuthentication("AccessToken");
        tokenAuth.setApiKey(accessToken);
        return client;
    }

    private String apiBasePath(String baseUrl) {
        String normalizedBaseUrl = baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        if (normalizedBaseUrl.endsWith("/api/v1")) {
            return normalizedBaseUrl;
        }
        return normalizedBaseUrl + "/api/v1";
    }
}
