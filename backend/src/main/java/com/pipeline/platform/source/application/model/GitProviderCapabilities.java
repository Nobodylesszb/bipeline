package com.pipeline.platform.source.application.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Git 平台连接能力")
public record GitProviderCapabilities(
        @Schema(description = "是否支持查询仓库列表")
        boolean listRepositories,
        @Schema(description = "是否支持读取分支")
        boolean readBranches,
        @Schema(description = "是否支持读取标签")
        boolean readTags,
        @Schema(description = "是否支持读取提交版本")
        boolean readRevision
) {

    public static GitProviderCapabilities basicGitLabApi() {
        return new GitProviderCapabilities(true, true, true, true);
    }

    public static GitProviderCapabilities basicGiteaApi() {
        return new GitProviderCapabilities(true, true, true, true);
    }

    public static GitProviderCapabilities none() {
        return new GitProviderCapabilities(false, false, false, false);
    }
}
