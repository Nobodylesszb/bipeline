package com.pipeline.platform.source.api;

import com.pipeline.platform.source.domain.AuthType;
import com.pipeline.platform.source.domain.CodeSourceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCodeSourceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull CodeSourceProvider provider,
        @NotBlank @Size(max = 500) String baseUrl,
        @NotNull AuthType authType,
        @Size(max = 200) String username,
        String secret
) {
}
