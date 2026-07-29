package com.pipeline.platform.source.application.model;

public record GitBranchInfo(
        String name,
        String commitId
) {
}
