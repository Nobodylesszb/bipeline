package com.pipeline.platform.project.application.service;

import java.util.List;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.CodeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class GitProviderResolver {

    @Autowired
    private List<GitProviderClient> gitProviderClients;

    GitProviderClient resolve(CodeSource codeSource) {
        return gitProviderClients.stream()
                .filter(client -> client.supports(codeSource.provider()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REPOSITORY_NOT_ACCESSIBLE,
                        "Code source provider is not supported yet"
                ));
    }
}
