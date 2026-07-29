package com.pipeline.platform.project.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "查询项目列表请求。当前 MVP 暂无筛选条件，保持 POST 形态方便后续扩展。")
public record ListProjectsRequest() {
}
