package com.pipeline.platform.source.application.service;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.application.command.CreateCodeSourceCommand;
import com.pipeline.platform.source.application.model.CodeSourceView;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateCodeSourceService {

    @Autowired
    private CodeSourceRepository codeSourceRepository;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional(rollbackFor = Exception.class)
    public CodeSourceView create(CreateCodeSourceCommand command) {
        if (codeSourceRepository.existsByName(command.name())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Code source name already exists"
            );
        }

        CodeSource codeSource = CodeSource.create(
                null,
                command.name(),
                command.provider(),
                command.baseUrl(),
                command.authType(),
                command.username(),
                command.secret(),
                clockProvider.now()
        );

        return CodeSourceView.from(codeSourceRepository.save(codeSource));
    }
}
