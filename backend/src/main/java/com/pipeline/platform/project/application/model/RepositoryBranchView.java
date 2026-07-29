package com.pipeline.platform.project.application.model;

import com.pipeline.platform.source.application.model.GitBranchInfo;

public record RepositoryBranchView(
        String name,
        String commitId
) {

    public static RepositoryBranchView from(GitBranchInfo branchInfo) {
        return new RepositoryBranchView(branchInfo.name(), branchInfo.commitId());
    }
}
