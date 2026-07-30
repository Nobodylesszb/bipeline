package com.pipeline.platform.jenkins.infrastructure.remote;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildLaunch;
import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.jenkins.application.model.JenkinsBuildSnapshot;
import com.pipeline.platform.jenkins.application.model.JenkinsJobDefinition;
import com.pipeline.platform.jenkins.application.model.JenkinsVerification;
import com.pipeline.platform.jenkins.application.port.JenkinsClient;
import com.pipeline.platform.jenkins.domain.JenkinsConnection;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JenkinsRemoteClient implements JenkinsClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int QUEUE_POLL_ATTEMPTS = 10;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public JenkinsVerification verify(JenkinsConnection connection) {
        try {
            HttpResponse<String> response = httpClient().send(
                    verifyRequest(connection),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return JenkinsVerification.verified("Jenkins connection is accessible");
            }
            return JenkinsVerification.failed("Jenkins verification failed with HTTP " + response.statusCode());
        } catch (IOException exception) {
            return JenkinsVerification.failed("Jenkins verification failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return JenkinsVerification.failed("Jenkins verification was interrupted");
        } catch (IllegalArgumentException exception) {
            return JenkinsVerification.failed("Jenkins base URL is invalid");
        }
    }

    @Override
    public JenkinsBuildLaunch createOrUpdateFreestyleJobAndBuild(
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition
    ) {
        try {
            String crumbHeader = crumbHeader(connection).orElse(null);
            createOrUpdateJob(connection, jobDefinition, crumbHeader);
            HttpResponse<String> buildResponse = httpClient().send(
                    buildRequest(connection, jobDefinition.name(), crumbHeader),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (buildResponse.statusCode() < 200 || buildResponse.statusCode() >= 300) {
                throw executionFailed("Jenkins build trigger failed with HTTP " + buildResponse.statusCode());
            }
            String queueUrl = buildResponse.headers()
                    .firstValue("Location")
                    .orElse(null);
            return new JenkinsBuildLaunch(queueUrl, waitForBuildNumber(connection, queueUrl));
        } catch (IOException exception) {
            throw executionFailed("Jenkins request failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw executionFailed("Jenkins request was interrupted");
        } catch (IllegalArgumentException exception) {
            throw executionFailed("Jenkins base URL is invalid");
        }
    }

    @Override
    public JenkinsBuildSnapshot getBuild(
            JenkinsConnection connection,
            String jobName,
            Integer buildNumber
    ) {
        if (buildNumber == null) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ENGINE_UNAVAILABLE,
                    "Jenkins build number is not available yet"
            );
        }
        try {
            HttpResponse<String> response = httpClient().send(
                    buildStatusRequest(connection, jobName, buildNumber),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw executionFailed("Jenkins build status failed with HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String result = root.path("result").isNull() ? null : root.path("result").asText(null);
            return new JenkinsBuildSnapshot(root.path("building").asBoolean(false), result);
        } catch (IOException exception) {
            throw executionFailed("Jenkins request failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw executionFailed("Jenkins request was interrupted");
        }
    }

    @Override
    public JenkinsConsoleLog getConsoleLog(
            JenkinsConnection connection,
            String jobName,
            Integer buildNumber
    ) {
        if (buildNumber == null) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ENGINE_UNAVAILABLE,
                    "Jenkins build number is not available yet"
            );
        }
        try {
            String externalUrl = consoleTextUrl(connection, jobName, buildNumber);
            HttpResponse<String> response = httpClient().send(
                    consoleTextRequest(connection, externalUrl),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw executionFailed("Jenkins console log request failed with HTTP " + response.statusCode());
            }
            return JenkinsConsoleLog.of(response.body(), externalUrl);
        } catch (IOException exception) {
            throw executionFailed("Jenkins request failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw executionFailed("Jenkins request was interrupted");
        }
    }

    @Override
    public Integer resolveBuildNumberFromQueue(JenkinsConnection connection, String queueUrl) {
        try {
            return buildNumberFromQueue(connection, queueUrl);
        } catch (IOException exception) {
            throw executionFailed("Jenkins queue request failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw executionFailed("Jenkins queue request was interrupted");
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    private HttpRequest verifyRequest(JenkinsConnection connection) {
        return HttpRequest.newBuilder()
                .uri(URI.create(connection.baseUrl() + "/api/json"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .GET()
                .build();
    }

    private Optional<String> crumbHeader(JenkinsConnection connection) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(connection.baseUrl() + "/crumbIssuer/api/json"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw executionFailed("Jenkins crumb request failed with HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return Optional.of(root.path("crumbRequestField").asText() + ": " + root.path("crumb").asText());
    }

    private void createOrUpdateJob(
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition,
            String crumbHeader
    ) throws IOException, InterruptedException {
        HttpResponse<String> existingJob = httpClient().send(
                jobExistsRequest(connection, jobDefinition.name()),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpRequest request;
        if (existingJob.statusCode() == 404) {
            request = createJobRequest(connection, jobDefinition, crumbHeader);
        } else if (existingJob.statusCode() >= 200 && existingJob.statusCode() < 300) {
            request = updateJobRequest(connection, jobDefinition, crumbHeader);
        } else {
            throw executionFailed("Jenkins job lookup failed with HTTP " + existingJob.statusCode());
        }
        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw executionFailed("Jenkins job save failed with HTTP " + response.statusCode());
        }
    }

    private HttpRequest jobExistsRequest(JenkinsConnection connection, String jobName) {
        return HttpRequest.newBuilder()
                .uri(URI.create(connection.baseUrl() + "/job/" + encodePath(jobName) + "/api/json"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .GET()
                .build();
    }

    private HttpRequest createJobRequest(
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition,
            String crumbHeader
    ) {
        return authenticatedXmlPost(
                connection,
                connection.baseUrl() + "/createItem?name=" + encodeQuery(jobDefinition.name()),
                crumbHeader,
                freestyleConfigXml(jobDefinition)
        );
    }

    private HttpRequest updateJobRequest(
            JenkinsConnection connection,
            JenkinsJobDefinition jobDefinition,
            String crumbHeader
    ) {
        return authenticatedXmlPost(
                connection,
                connection.baseUrl() + "/job/" + encodePath(jobDefinition.name()) + "/config.xml",
                crumbHeader,
                freestyleConfigXml(jobDefinition)
        );
    }

    private HttpRequest buildRequest(JenkinsConnection connection, String jobName, String crumbHeader) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(connection.baseUrl() + "/job/" + encodePath(jobName) + "/build"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .POST(HttpRequest.BodyPublishers.noBody());
        addCrumb(builder, crumbHeader);
        return builder.build();
    }

    private HttpRequest buildStatusRequest(JenkinsConnection connection, String jobName, Integer buildNumber) {
        return HttpRequest.newBuilder()
                .uri(URI.create(connection.baseUrl()
                        + "/job/"
                        + encodePath(jobName)
                        + "/"
                        + buildNumber
                        + "/api/json"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .GET()
                .build();
    }

    private HttpRequest consoleTextRequest(JenkinsConnection connection, String consoleTextUrl) {
        return HttpRequest.newBuilder()
                .uri(URI.create(consoleTextUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .GET()
                .build();
    }

    private String consoleTextUrl(JenkinsConnection connection, String jobName, Integer buildNumber) {
        return connection.baseUrl()
                + "/job/"
                + encodePath(jobName)
                + "/"
                + buildNumber
                + "/consoleText";
    }

    private HttpRequest queueItemRequest(JenkinsConnection connection, String queueUrl) {
        return HttpRequest.newBuilder()
                .uri(URI.create(queueUrl + "api/json"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .GET()
                .build();
    }

    private HttpRequest authenticatedXmlPost(
            JenkinsConnection connection,
            String url,
            String crumbHeader,
            String body
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", basicAuth(connection.username(), connection.apiTokenPlain()))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        addCrumb(builder, crumbHeader);
        return builder.build();
    }

    private Integer waitForBuildNumber(JenkinsConnection connection, String queueUrl)
            throws IOException, InterruptedException {
        if (queueUrl == null || queueUrl.isBlank()) {
            return null;
        }
        for (int attempt = 0; attempt < QUEUE_POLL_ATTEMPTS; attempt++) {
            Thread.sleep(500);
            Integer buildNumber = buildNumberFromQueue(connection, queueUrl);
            if (buildNumber != null) {
                return buildNumber;
            }
        }
        return null;
    }

    private Integer buildNumberFromQueue(JenkinsConnection connection, String queueUrl)
            throws IOException, InterruptedException {
        if (queueUrl == null || queueUrl.isBlank()) {
            return null;
        }
        HttpResponse<String> response = httpClient().send(
                queueItemRequest(connection, queueUrl),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        JsonNode executable = objectMapper.readTree(response.body()).path("executable");
        if (executable.has("number")) {
            return executable.path("number").asInt();
        }
        return null;
    }

    private void addCrumb(HttpRequest.Builder builder, String crumbHeader) {
        if (crumbHeader == null || crumbHeader.isBlank()) {
            return;
        }
        int splitAt = crumbHeader.indexOf(':');
        if (splitAt > 0) {
            builder.header(crumbHeader.substring(0, splitAt), crumbHeader.substring(splitAt + 1).trim());
        }
    }

    private String basicAuth(String username, String apiToken) {
        String credential = username + ":" + (apiToken == null ? "" : apiToken);
        return "Basic " + Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    }

    private String freestyleConfigXml(JenkinsJobDefinition jobDefinition) {
        return """
                <project>
                  <actions/>
                  <description>%s</description>
                  <keepDependencies>false</keepDependencies>
                  <properties/>
                  <scm class="hudson.scm.NullSCM"/>
                  <canRoam>true</canRoam>
                  <disabled>false</disabled>
                  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
                  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
                  <triggers/>
                  <concurrentBuild>false</concurrentBuild>
                  <builders>
                    <hudson.tasks.Shell>
                      <command>%s</command>
                      <configuredLocalRules/>
                    </hudson.tasks.Shell>
                  </builders>
                  <publishers/>
                  <buildWrappers/>
                </project>
                """.formatted(escapeXml(jobDefinition.description()), escapeXml(jobDefinition.shellScript()));
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private BusinessException executionFailed(String message) {
        return new BusinessException(ErrorCode.EXECUTION_ENGINE_UNAVAILABLE, message);
    }
}
