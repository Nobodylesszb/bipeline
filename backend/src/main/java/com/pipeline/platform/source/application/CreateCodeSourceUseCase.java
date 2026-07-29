package com.pipeline.platform.source.application;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.id.IdGenerator;
import com.pipeline.platform.shared.time.ClockProvider;
import com.pipeline.platform.source.domain.CodeSource;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateCodeSourceUseCase {

    private final CodeSourceRepository codeSourceRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clockProvider;

    public CreateCodeSourceUseCase(
            CodeSourceRepository codeSourceRepository,
            IdGenerator idGenerator,
            ClockProvider clockProvider
    ) {
        this.codeSourceRepository = codeSourceRepository;
        this.idGenerator = idGenerator;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public CodeSourceView create(CreateCodeSourceCommand command) {
        if (codeSourceRepository.existsByName(command.name())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Code source name already exists"
            );
        }

        CodeSource codeSource = CodeSource.create(
                idGenerator.nextId(),
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
