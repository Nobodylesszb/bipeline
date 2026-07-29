package com.pipeline.platform.project.api.controller;

import com.pipeline.platform.project.api.mapper.ProjectResponseMapper;
import com.pipeline.platform.project.api.request.ListRepositoryBranchesRequest;
import com.pipeline.platform.project.api.request.VerifyRepositoryRequest;
import com.pipeline.platform.project.api.response.RepositoryBranchListResponse;
import com.pipeline.platform.project.api.response.RepositoryVerificationResponse;
import com.pipeline.platform.project.application.command.ListRepositoryBranchesCommand;
import com.pipeline.platform.project.application.command.VerifyRepositoryCommand;
import com.pipeline.platform.project.application.service.ListRepositoryBranchesService;
import com.pipeline.platform.project.application.service.VerifyRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/code-sources/repository")
@Tag(name = "代码仓库", description = "基于已保存的代码源连接，验证仓库并读取分支。")
public class ProjectRepositoryController {

    @Autowired
    private VerifyRepositoryService verifyRepositoryService;

    @Autowired
    private ListRepositoryBranchesService listRepositoryBranchesService;

    @Autowired
    private ProjectResponseMapper responseMapper;

    @PostMapping("/verify")
    @Operation(summary = "验证指定仓库", description = "使用已保存的代码源 Token 读取 owner/repo 仓库信息。")
    public RepositoryVerificationResponse verify(@Valid @RequestBody VerifyRepositoryRequest request) {
        return responseMapper.toRepositoryVerificationResponse(verifyRepositoryService.verify(new VerifyRepositoryCommand(
                request.codeSourceId(),
                request.repositoryPath()
        )));
    }

    @PostMapping("/branches")
    @Operation(summary = "查询仓库分支", description = "用于创建流水线或绑定项目仓库时选择默认分支。")
    public RepositoryBranchListResponse branches(@Valid @RequestBody ListRepositoryBranchesRequest request) {
        return new RepositoryBranchListResponse(
                listRepositoryBranchesService.findBranches(new ListRepositoryBranchesCommand(
                                request.codeSourceId(),
                                request.repositoryPath()
                        ))
                        .stream()
                        .map(responseMapper::toBranchResponse)
                        .toList()
        );
    }
}
