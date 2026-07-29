package com.pipeline.platform.source.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeSourceRepository {

    boolean existsByName(String name);

    CodeSource save(CodeSource codeSource);

    Optional<CodeSource> findById(UUID id);

    List<CodeSource> findAll();
}
