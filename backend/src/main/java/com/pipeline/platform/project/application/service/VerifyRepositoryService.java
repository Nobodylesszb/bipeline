package com.pipeline.platform.project.application.service;

import com.pipeline.platform.project.application.command.VerifyRepositoryCommand;
import com.pipeline.platform.project.application.model.RepositoryVerificationView;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.CodeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VerifyRepositoryService {

    @Autowired
    private CodeSourceGuard codeSourceGuard;

    @Autowired
    private GitProviderResolver gitProviderResolver;

    @Transactional(readOnly = true)
    public RepositoryVerificationView verify(VerifyRepositoryCommand command) {
        CodeSource codeSource = codeSourceGuard.requireVerified(command.codeSourceId());
        GitProviderClient gitProviderClient = gitProviderResolver.resolve(codeSource);
        return RepositoryVerificationView.accessible(gitProviderClient.getRepository(
                codeSource,
                command.repositoryPath()
        ));
    }
}
