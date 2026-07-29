package com.pipeline.platform.source.domain;

import java.util.List;
import java.util.Optional;

public interface CodeSourceRepository {

    boolean existsByName(String name);

    CodeSource save(CodeSource codeSource);

    Optional<CodeSource> findById(Long id);

    List<CodeSource> findAll();
}
