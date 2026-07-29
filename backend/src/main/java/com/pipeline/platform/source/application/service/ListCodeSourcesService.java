package com.pipeline.platform.source.application.service;

import java.util.List;

import com.pipeline.platform.source.application.model.CodeSourceView;
import com.pipeline.platform.source.domain.CodeSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListCodeSourcesService {

    @Autowired
    private CodeSourceRepository codeSourceRepository;

    @Transactional(readOnly = true)
    public List<CodeSourceView> findAll() {
        return codeSourceRepository.findAll()
                .stream()
                .map(CodeSourceView::from)
                .toList();
    }
}
