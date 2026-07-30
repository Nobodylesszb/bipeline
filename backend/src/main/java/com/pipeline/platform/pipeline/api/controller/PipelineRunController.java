package com.pipeline.platform.pipeline.api.controller;

import com.pipeline.platform.pipeline.api.mapper.PipelineRunResponseMapper;
import com.pipeline.platform.pipeline.api.request.ListPipelineRunsRequest;
import com.pipeline.platform.pipeline.api.request.PipelineRunDetailRequest;
import com.pipeline.platform.pipeline.api.request.StartPipelineRunRequest;
import com.pipeline.platform.pipeline.api.response.PipelineRunListResponse;
import com.pipeline.platform.pipeline.api.response.PipelineRunResponse;
import com.pipeline.platform.pipeline.application.command.ListPipelineRunsCommand;
import com.pipeline.platform.pipeline.application.command.PipelineRunDetailCommand;
import com.pipeline.platform.pipeline.application.service.GetPipelineRunDetailService;
import com.pipeline.platform.pipeline.application.service.ListPipelineRunsService;
import com.pipeline.platform.pipeline.application.service.StartPipelineRunService;
import com.pipeline.platform.pipeline.application.service.SyncPipelineRunStatusService;
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
@RequestMapping("/api/v1/pipeline-runs")
@Tag(name = "流水线运行", description = "通过 Jenkins 启动流水线运行并同步运行状态。")
public class PipelineRunController {

    @Autowired
    private StartPipelineRunService startPipelineRunService;

    @Autowired
    private ListPipelineRunsService listPipelineRunsService;

    @Autowired
    private GetPipelineRunDetailService getPipelineRunDetailService;

    @Autowired
    private SyncPipelineRunStatusService syncPipelineRunStatusService;

    @Autowired
    private PipelineRunResponseMapper responseMapper;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "启动流水线运行", description = "创建或更新 Jenkins Job，并触发一次构建。")
    public PipelineRunResponse start(@Valid @RequestBody StartPipelineRunRequest request) {
        return responseMapper.toResponse(startPipelineRunService.start(responseMapper.toCommand(request)));
    }

    @PostMapping("/list")
    @Operation(summary = "查询流水线运行列表")
    public PipelineRunListResponse list(@Valid @RequestBody ListPipelineRunsRequest request) {
        return new PipelineRunListResponse(
                listPipelineRunsService.findAll(new ListPipelineRunsCommand(request.pipelineId()))
                        .stream()
                        .map(responseMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/detail")
    @Operation(summary = "查询流水线运行详情")
    public PipelineRunResponse detail(@Valid @RequestBody PipelineRunDetailRequest request) {
        return responseMapper.toResponse(getPipelineRunDetailService.get(new PipelineRunDetailCommand(
                request.pipelineRunId()
        )));
    }

    @PostMapping("/sync-status")
    @Operation(summary = "同步流水线运行状态", description = "从 Jenkins Build API 拉取构建结果并回写运行状态。")
    public PipelineRunResponse syncStatus(@Valid @RequestBody PipelineRunDetailRequest request) {
        return responseMapper.toResponse(syncPipelineRunStatusService.sync(new PipelineRunDetailCommand(
                request.pipelineRunId()
        )));
    }
}
