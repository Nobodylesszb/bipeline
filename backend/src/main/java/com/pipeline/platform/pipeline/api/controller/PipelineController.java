package com.pipeline.platform.pipeline.api.controller;

import com.pipeline.platform.pipeline.api.mapper.PipelineResponseMapper;
import com.pipeline.platform.pipeline.api.request.CreatePipelineRequest;
import com.pipeline.platform.pipeline.api.request.ListPipelinesRequest;
import com.pipeline.platform.pipeline.api.request.PipelineDetailRequest;
import com.pipeline.platform.pipeline.api.request.PipelineStatusChangeRequest;
import com.pipeline.platform.pipeline.api.response.PipelineListResponse;
import com.pipeline.platform.pipeline.api.response.PipelineResponse;
import com.pipeline.platform.pipeline.application.command.ChangePipelineStatusCommand;
import com.pipeline.platform.pipeline.application.command.GetPipelineDetailCommand;
import com.pipeline.platform.pipeline.application.command.ListPipelinesCommand;
import com.pipeline.platform.pipeline.application.service.ChangePipelineStatusService;
import com.pipeline.platform.pipeline.application.service.CreatePipelineService;
import com.pipeline.platform.pipeline.application.service.GetPipelineDetailService;
import com.pipeline.platform.pipeline.application.service.ListPipelinesService;
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
@RequestMapping("/api/v1/pipelines")
@Tag(name = "流水线", description = "创建、查询、启用和禁用 CI 流水线配置。第一版只保存配置，不执行 Jenkins。")
public class PipelineController {

    @Autowired
    private CreatePipelineService createPipelineService;

    @Autowired
    private ListPipelinesService listPipelinesService;

    @Autowired
    private GetPipelineDetailService getPipelineDetailService;

    @Autowired
    private ChangePipelineStatusService changePipelineStatusService;

    @Autowired
    private PipelineResponseMapper responseMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建流水线", description = "创建 Pipeline/Stage/Step 配置，状态默认为 DRAFT。")
    public PipelineResponse create(@Valid @RequestBody CreatePipelineRequest request) {
        return responseMapper.toResponse(createPipelineService.create(responseMapper.toCommand(request)));
    }

    @PostMapping("/list")
    @Operation(summary = "查询流水线列表")
    public PipelineListResponse list(@Valid @RequestBody ListPipelinesRequest request) {
        return new PipelineListResponse(
                listPipelinesService.findAll(new ListPipelinesCommand(request.projectId()))
                        .stream()
                        .map(responseMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/detail")
    @Operation(summary = "查询流水线详情")
    public PipelineResponse detail(@Valid @RequestBody PipelineDetailRequest request) {
        return responseMapper.toResponse(getPipelineDetailService.get(new GetPipelineDetailCommand(
                request.pipelineId()
        )));
    }

    @PostMapping("/activate")
    @Operation(summary = "启用流水线")
    public PipelineResponse activate(@Valid @RequestBody PipelineStatusChangeRequest request) {
        return responseMapper.toResponse(changePipelineStatusService.activate(new ChangePipelineStatusCommand(
                request.pipelineId()
        )));
    }

    @PostMapping("/disable")
    @Operation(summary = "禁用流水线")
    public PipelineResponse disable(@Valid @RequestBody PipelineStatusChangeRequest request) {
        return responseMapper.toResponse(changePipelineStatusService.disable(new ChangePipelineStatusCommand(
                request.pipelineId()
        )));
    }
}
