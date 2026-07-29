package com.pipeline.platform.source.api;

import java.util.Map;

import com.pipeline.platform.source.application.CreateCodeSourceCommand;
import com.pipeline.platform.source.application.CreateCodeSourceUseCase;
import com.pipeline.platform.source.application.ListCodeSourcesQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/code-sources")
public class CodeSourceController {

    private final CreateCodeSourceUseCase createCodeSourceUseCase;
    private final ListCodeSourcesQuery listCodeSourcesQuery;
    private final CodeSourceResponseMapper responseMapper;

    public CodeSourceController(
            CreateCodeSourceUseCase createCodeSourceUseCase,
            ListCodeSourcesQuery listCodeSourcesQuery,
            CodeSourceResponseMapper responseMapper
    ) {
        this.createCodeSourceUseCase = createCodeSourceUseCase;
        this.listCodeSourcesQuery = listCodeSourcesQuery;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodeSourceResponse create(@Valid @RequestBody CreateCodeSourceRequest request) {
        return responseMapper.toResponse(createCodeSourceUseCase.create(toCommand(request)));
    }

    @GetMapping
    public Map<String, Object> findAll() {
        return Map.of(
                "items",
                listCodeSourcesQuery.findAll()
                        .stream()
                        .map(responseMapper::toResponse)
                        .toList()
        );
    }

    private CreateCodeSourceCommand toCommand(CreateCodeSourceRequest request) {
        return new CreateCodeSourceCommand(
                request.name(),
                request.provider(),
                request.baseUrl(),
                request.authType(),
                request.username(),
                request.secret()
        );
    }
}
