package com.pipeline.platform.jenkins.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "查询 Jenkins 连接列表请求。当前 MVP 暂无筛选条件。")
public record ListJenkinsConnectionsRequest() {
}
