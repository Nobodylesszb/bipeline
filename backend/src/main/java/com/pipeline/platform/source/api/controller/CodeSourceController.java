package com.pipeline.platform.source.api.controller;

import com.pipeline.platform.source.api.mapper.CodeSourceResponseMapper;
import com.pipeline.platform.source.api.request.CreateCodeSourceRequest;
import com.pipeline.platform.source.api.request.VerifyCodeSourceRequest;
import com.pipeline.platform.source.api.response.CodeSourceListResponse;
import com.pipeline.platform.source.api.response.CodeSourceResponse;
import com.pipeline.platform.source.api.response.CodeSourceVerificationResponse;
import com.pipeline.platform.source.application.command.CreateCodeSourceCommand;
import com.pipeline.platform.source.application.command.VerifyCodeSourceCommand;
import com.pipeline.platform.source.application.service.CreateCodeSourceService;
import com.pipeline.platform.source.application.service.ListCodeSourcesService;
import com.pipeline.platform.source.application.service.VerifyCodeSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/code-sources")
@Tag(name = "代码源连接", description = "配置 GitLab/GitHub/Gitea 等代码平台连接。当前优先打通 GitLab 和 Gitea。")
public class CodeSourceController {

    @Autowired
    private CreateCodeSourceService createCodeSourceService;

    @Autowired
    private ListCodeSourcesService listCodeSourcesService;

    @Autowired
    private VerifyCodeSourceService verifyCodeSourceService;

    @Autowired
    private CodeSourceResponseMapper responseMapper;

    /*
     * 创建代码源连接。
     *
     * 用途：
     * - 先把 GitLab/GitHub/Gitea 等代码平台地址和认证信息保存到平台。
     * - 后面创建 Project 时，可以直接选择这个代码源，不需要重复填写 GitLab 地址和 Token。
     *
     * 当前已打通的 provider：
     * - GITLAB
     * - GITEA
     *
     * 当前 GitLab/Gitea 推荐 authType：
     * - ACCESS_TOKEN
     *
     * 示例：
     *
     * curl -X POST http://localhost:8100/api/v1/code-sources \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "name": "公司 GitLab",
     *     "provider": "GITLAB",
     *     "baseUrl": "https://gitlab.example.com",
     *     "authType": "ACCESS_TOKEN",
     *     "username": "bo",
     *     "secret": "你的 GitLab Access Token"
     *   }'
     *
     * Gitea 示例：
     *
     * curl -X POST http://localhost:8100/api/v1/code-sources \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "name": "本地 Gitea",
     *     "provider": "GITEA",
     *     "baseUrl": "http://localhost:3000",
     *     "authType": "ACCESS_TOKEN",
     *     "username": "bobo_3776",
     *     "secret": "你的 Gitea Personal Access Token"
     *   }'
     *
     * 注意：
     * - 本地 MVP 允许 secret 明文入库。
     * - 这里的 secret 是 Personal Access Token，不是 WebHook Secret。
     * - API 响应不会返回完整 secret，只返回 secretMasked 和 secretLastFour。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建代码源连接",
            description = "保存 GitLab/Gitea 等代码平台地址和 Token。响应会脱敏，不返回完整 secret。"
    )
    public CodeSourceResponse create(@Valid @RequestBody CreateCodeSourceRequest request) {
        return responseMapper.toResponse(createCodeSourceService.create(toCommand(request)));
    }

    /*
     * 查询已保存的代码源连接列表。
     *
     * 用途：
     * - 创建 Project 时，用这个接口展示可选代码源。
     * - 页面上可以展示 name、provider、baseUrl、verificationStatus。
     *
     * 示例：
     *
     * curl http://localhost:8100/api/v1/code-sources
     *
     * 返回示例：
     *
     * {
     *   "items": [
     *     {
     *       "id": 1,
     *       "name": "公司 GitLab",
     *       "provider": "GITLAB",
     *       "baseUrl": "https://gitlab.example.com",
     *       "authType": "ACCESS_TOKEN",
     *       "secretMasked": "********",
     *       "secretLastFour": "abcd",
     *       "verificationStatus": "VERIFIED"
     *     }
     *   ]
     * }
     */
    @GetMapping
    @Operation(
            summary = "查询代码源连接列表",
            description = "用于创建 Project 时展示可复用的 Git 连接。"
    )
    public CodeSourceListResponse findAll() {
        return new CodeSourceListResponse(
                listCodeSourcesService.findAll()
                .stream()
                .map(responseMapper::toResponse)
                .toList()
        );
    }

    /*
     * 测试代码源连通性。
     *
     * 用途：
     * - 用户保存 GitLab/Gitea 地址和 Token 后，点击“测试连接”。
     * - GitLab 使用 gitlab4j-api 验证当前用户和可访问项目。
     * - Gitea 使用 java-gitea-api 验证当前用户和可访问仓库。
     * - 成功后 code_sources.verification_status 会更新为 VERIFIED。
     * - 失败后 code_sources.verification_status 会更新为 FAILED，并返回业务错误。
     *
     * 示例：
     *
     * curl -X POST http://localhost:8100/api/v1/code-sources/{codeSourceId}/verification \
     *   -H "Content-Type: application/json" \
     *   -d '{}'
     *
     * 可选 repositoryPath：
     *
     * curl -X POST http://localhost:8100/api/v1/code-sources/{codeSourceId}/verification \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "repositoryPath": "group/order-service"
     *   }'
     *
     * 当前 repositoryPath 先预留，后面用于验证指定仓库是否可访问。
     */
    @PostMapping("/{codeSourceId}/verification")
    @Operation(
            summary = "测试代码源连通性",
            description = "当前 GitLab 使用 gitlab4j-api，Gitea 使用 java-gitea-api 验证 Token 是否有效，并确认能读取当前用户可访问项目。"
    )
    public CodeSourceVerificationResponse verify(
            @Parameter(description = "代码源连接 ID", required = true)
            @PathVariable Long codeSourceId,
            @Valid @RequestBody(required = false) VerifyCodeSourceRequest request
    ) {
        var result = verifyCodeSourceService.verify(new VerifyCodeSourceCommand(
                codeSourceId,
                request == null ? null : request.repositoryPath()
        ));
        return new CodeSourceVerificationResponse(
                result.status(),
                result.message(),
                result.capabilities(),
                result.verifiedAt()
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
