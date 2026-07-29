package com.pipeline.platform.source.application.port;

import java.util.List;

import com.pipeline.platform.source.application.model.GitBranchInfo;
import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.application.model.GitRepositoryInfo;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;

public interface GitProviderClient {

    boolean supports(CodeSourceProvider provider);

    GitProviderVerification verify(CodeSource codeSource, String repositoryPath);

    GitRepositoryInfo getRepository(CodeSource codeSource, String repositoryPath);

    List<GitBranchInfo> listBranches(CodeSource codeSource, String repositoryPath);
}
