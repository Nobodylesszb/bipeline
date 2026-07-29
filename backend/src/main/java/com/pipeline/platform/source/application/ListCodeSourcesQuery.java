package com.pipeline.platform.source.application;

import java.util.List;

import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListCodeSourcesQuery {

    private final CodeSourceRepository codeSourceRepository;

    public ListCodeSourcesQuery(CodeSourceRepository codeSourceRepository) {
        this.codeSourceRepository = codeSourceRepository;
    }

    @Transactional(readOnly = true)
    public List<CodeSourceView> findAll() {
        return codeSourceRepository.findAll()
                .stream()
                .map(CodeSourceView::from)
                .toList();
    }
}
