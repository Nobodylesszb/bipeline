package com.pipeline.platform.project.api.controller;

import com.pipeline.platform.project.api.mapper.ProjectResponseMapper;
import com.pipeline.platform.project.api.request.BindProjectRepositoryRequest;
import com.pipeline.platform.project.api.request.CreateProjectRequest;
import com.pipeline.platform.project.api.request.ListProjectsRequest;
import com.pipeline.platform.project.api.response.ProjectGitRepositoryResponse;
import com.pipeline.platform.project.api.response.ProjectListResponse;
import com.pipeline.platform.project.api.response.ProjectResponse;
import com.pipeline.platform.project.application.command.BindProjectRepositoryCommand;
import com.pipeline.platform.project.application.command.CreateProjectCommand;
import com.pipeline.platform.project.application.service.BindProjectRepositoryService;
import com.pipeline.platform.project.application.service.CreateProjectService;
import com.pipeline.platform.project.application.service.ListProjectsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "项目", description = "创建项目、查询项目，以及为项目绑定一个主代码仓库。")
public class ProjectController {

    @Autowired
    private CreateProjectService createProjectService;

    @Autowired
    private ListProjectsService listProjectsService;

    @Autowired
    private BindProjectRepositoryService bindProjectRepositoryService;

    @Autowired
    private ProjectResponseMapper responseMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建项目", description = "创建项目时选择一个已验证的代码源连接。第一版一个项目只绑定一个主仓库。")
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return responseMapper.toResponse(createProjectService.create(new CreateProjectCommand(
                request.name(),
                request.description(),
                request.codeSourceId()
        )));
    }

    @PostMapping("/list")
    @Operation(summary = "查询项目列表", description = "当前 MVP 使用 POST 查询，后续可以自然增加筛选条件。")
    public ProjectListResponse list(@RequestBody(required = false) ListProjectsRequest request) {
        return new ProjectListResponse(
                listProjectsService.findAll()
                        .stream()
                        .map(responseMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/repository/bind")
    @Operation(summary = "绑定项目主仓库", description = "验证仓库和默认分支后，将一个主仓库绑定到项目。重复绑定会更新原绑定。")
    public ProjectGitRepositoryResponse bindRepository(@Valid @RequestBody BindProjectRepositoryRequest request) {
        return responseMapper.toRepositoryResponse(bindProjectRepositoryService.bind(new BindProjectRepositoryCommand(
                request.projectId(),
                request.codeSourceId(),
                request.repositoryPath(),
                request.defaultBranch(),
                request.contextDirectory()
        )));
    }
}
