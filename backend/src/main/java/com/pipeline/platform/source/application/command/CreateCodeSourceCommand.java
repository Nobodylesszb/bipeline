package com.pipeline.platform.source.application.command;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;

public record CreateCodeSourceCommand(
        String name,
        CodeSourceProvider provider,
        String baseUrl,
        AuthType authType,
        String username,
        String secret
) {
}
