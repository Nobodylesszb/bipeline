package com.pipeline.platform.source.application.port;

import com.pipeline.platform.source.application.model.GitProviderVerification;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceProvider;

public interface GitProviderClient {

    boolean supports(CodeSourceProvider provider);

    GitProviderVerification verify(CodeSource codeSource, String repositoryPath);
}
