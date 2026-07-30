package com.pipeline.platform.jenkins.api.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jenkins 连接列表响应")
public record JenkinsConnectionListResponse(
        List<JenkinsConnectionResponse> items
) {
}
