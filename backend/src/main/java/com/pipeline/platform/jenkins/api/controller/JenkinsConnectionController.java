package com.pipeline.platform.jenkins.api.controller;

import com.pipeline.platform.jenkins.api.mapper.JenkinsConnectionResponseMapper;
import com.pipeline.platform.jenkins.api.request.CreateJenkinsConnectionRequest;
import com.pipeline.platform.jenkins.api.request.ListJenkinsConnectionsRequest;
import com.pipeline.platform.jenkins.api.request.VerifyJenkinsConnectionRequest;
import com.pipeline.platform.jenkins.api.response.JenkinsConnectionListResponse;
import com.pipeline.platform.jenkins.api.response.JenkinsConnectionResponse;
import com.pipeline.platform.jenkins.api.response.JenkinsConnectionVerificationResponse;
import com.pipeline.platform.jenkins.application.command.CreateJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.command.VerifyJenkinsConnectionCommand;
import com.pipeline.platform.jenkins.application.service.CreateJenkinsConnectionService;
import com.pipeline.platform.jenkins.application.service.ListJenkinsConnectionsService;
import com.pipeline.platform.jenkins.application.service.VerifyJenkinsConnectionService;
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
@RequestMapping("/api/v1/jenkins-connections")
@Tag(name = "Jenkins 连接", description = "保存、查询和验证 Jenkins Remote API 连接。")
public class JenkinsConnectionController {

    @Autowired
    private CreateJenkinsConnectionService createJenkinsConnectionService;

    @Autowired
    private ListJenkinsConnectionsService listJenkinsConnectionsService;

    @Autowired
    private VerifyJenkinsConnectionService verifyJenkinsConnectionService;

    @Autowired
    private JenkinsConnectionResponseMapper responseMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建 Jenkins 连接", description = "第一版本地 MVP 明文保存 API Token，响应中只返回脱敏信息。")
    public JenkinsConnectionResponse create(@Valid @RequestBody CreateJenkinsConnectionRequest request) {
        return responseMapper.toResponse(createJenkinsConnectionService.create(new CreateJenkinsConnectionCommand(
                request.name(),
                request.baseUrl(),
                request.username(),
                request.apiToken()
        )));
    }

    @PostMapping("/list")
    @Operation(summary = "查询 Jenkins 连接列表")
    public JenkinsConnectionListResponse list(@RequestBody(required = false) ListJenkinsConnectionsRequest request) {
        return new JenkinsConnectionListResponse(
                listJenkinsConnectionsService.findAll()
                        .stream()
                        .map(responseMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/verify")
    @Operation(summary = "验证 Jenkins 连接", description = "调用 Jenkins Remote API 的 /api/json 验证地址、用户名和 Token。")
    public JenkinsConnectionVerificationResponse verify(@Valid @RequestBody VerifyJenkinsConnectionRequest request) {
        return responseMapper.toVerificationResponse(verifyJenkinsConnectionService.verify(
                new VerifyJenkinsConnectionCommand(request.connectionId())
        ));
    }
}
