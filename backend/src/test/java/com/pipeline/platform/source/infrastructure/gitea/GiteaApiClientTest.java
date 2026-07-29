package com.pipeline.platform.source.infrastructure.gitea;

import static org.assertj.core.api.Assertions.assertThat;

import io.gitea.ApiClient;
import io.gitea.auth.ApiKeyAuth;
import org.junit.jupiter.api.Test;

class GiteaApiClientTest {

    @Test
    void createsClientWithApiBasePathAndAccessToken() {
        GiteaApiClient factory = new GiteaApiClient();

        ApiClient client = factory.create("http://localhost:3000/", "token-for-test");

        assertThat(client.getBasePath()).isEqualTo("http://localhost:3000/api/v1");
        ApiKeyAuth tokenAuth = (ApiKeyAuth) client.getAuthentication("AccessToken");
        assertThat(tokenAuth.getApiKey()).isEqualTo("token-for-test");
    }

    @Test
    void keepsApiBasePathWhenAlreadyProvided() {
        GiteaApiClient factory = new GiteaApiClient();

        ApiClient client = factory.create("http://localhost:3000/api/v1", "token-for-test");

        assertThat(client.getBasePath()).isEqualTo("http://localhost:3000/api/v1");
    }
}
