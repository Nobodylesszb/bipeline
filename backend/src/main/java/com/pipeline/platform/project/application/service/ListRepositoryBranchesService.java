package com.pipeline.platform.project.application.service;

import java.util.List;

import com.pipeline.platform.project.application.command.ListRepositoryBranchesCommand;
import com.pipeline.platform.project.application.model.RepositoryBranchView;
import com.pipeline.platform.source.application.port.GitProviderClient;
import com.pipeline.platform.source.domain.CodeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListRepositoryBranchesService {

    @Autowired
    private CodeSourceGuard codeSourceGuard;

    @Autowired
    private GitProviderResolver gitProviderResolver;

    @Transactional(readOnly = true)
    public List<RepositoryBranchView> findBranches(ListRepositoryBranchesCommand command) {
        CodeSource codeSource = codeSourceGuard.requireVerified(command.codeSourceId());
        GitProviderClient gitProviderClient = gitProviderResolver.resolve(codeSource);
        return gitProviderClient.listBranches(codeSource, command.repositoryPath())
                .stream()
                .map(RepositoryBranchView::from)
                .toList();
    }
}
