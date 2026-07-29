package com.pipeline.platform.source.application.model;

import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;

public record RepositoryPath(
        String owner,
        String name
) {

    public static RepositoryPath parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Repository path is required"
            );
        }

        String normalized = value.trim();
        String[] parts = normalized.split("/");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Repository path must use owner/repo format"
            );
        }

        return new RepositoryPath(parts[0], parts[1]);
    }

    public String value() {
        return owner + "/" + name;
    }
}
