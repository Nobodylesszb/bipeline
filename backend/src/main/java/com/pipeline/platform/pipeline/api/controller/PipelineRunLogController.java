package com.pipeline.platform.pipeline.api.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.pipeline.api.mapper.PipelineRunLogResponseMapper;
import com.pipeline.platform.pipeline.api.request.LatestProjectPipelineRunLogRequest;
import com.pipeline.platform.pipeline.api.request.PipelineRunLogRequest;
import com.pipeline.platform.pipeline.api.response.PipelineRunLogResponse;
import com.pipeline.platform.pipeline.application.command.PipelineRunLogCommand;
import com.pipeline.platform.pipeline.application.service.CapturePipelineRunLogService;
import com.pipeline.platform.pipeline.application.service.GetPipelineRunLogService;
import com.pipeline.platform.pipeline.application.service.LatestPipelineRunFinder;
import com.pipeline.platform.pipeline.application.service.PipelineRunLogFetcher;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/pipeline-run-logs")
@Tag(name = "流水线运行日志", description = "保存最后日志，并支持构建过程日志流。")
public class PipelineRunLogController {

    private static final long STREAM_TIMEOUT_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int STREAM_POLL_LIMIT = 600;
    private static final int STREAM_POLL_INTERVAL_MILLIS = 1000;

    @Autowired
    private CapturePipelineRunLogService capturePipelineRunLogService;

    @Autowired
    private GetPipelineRunLogService getPipelineRunLogService;

    @Autowired
    private PipelineRunLogFetcher pipelineRunLogFetcher;

    @Autowired
    private LatestPipelineRunFinder latestPipelineRunFinder;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunLogResponseMapper responseMapper;

    @PostMapping("/capture")
    @Operation(summary = "保存流水线最后日志", description = "从 Jenkins 拉取 consoleText，保存最后 200 行到数据库。")
    public PipelineRunLogResponse capture(@Valid @RequestBody PipelineRunLogRequest request) {
        return responseMapper.toResponse(capturePipelineRunLogService.capture(responseMapper.toCommand(request)));
    }

    @PostMapping("/latest")
    @Operation(summary = "查询最近一次保存的流水线日志")
    public PipelineRunLogResponse latest(@Valid @RequestBody PipelineRunLogRequest request) {
        return responseMapper.toResponse(getPipelineRunLogService.get(responseMapper.toCommand(request)));
    }

    @PostMapping("/full")
    @Operation(summary = "拉取 Jenkins 完整日志", description = "从 Jenkins 实时拉取完整 consoleText；这个接口不落库。")
    public PipelineRunLogResponse full(@Valid @RequestBody PipelineRunLogRequest request) {
        PipelineRun pipelineRun = loadPipelineRun(responseMapper.toCommand(request));
        JenkinsConsoleLog consoleLog = pipelineRunLogFetcher.fetch(pipelineRun);
        return responseMapper.toFullResponse(pipelineRun.id(), consoleLog);
    }

    @PostMapping("/latest-full")
    @Operation(summary = "拉取项目最近一次完整日志", description = "只需要 projectId；可选 pipelineId 限定某条流水线。")
    public PipelineRunLogResponse latestFull(@Valid @RequestBody LatestProjectPipelineRunLogRequest request) {
        PipelineRun pipelineRun = latestPipelineRunFinder.find(responseMapper.toCommand(request));
        JenkinsConsoleLog consoleLog = pipelineRunLogFetcher.fetch(pipelineRun);
        return responseMapper.toFullResponse(pipelineRun.id(), consoleLog);
    }

    @PostMapping("/capture-latest")
    @Operation(summary = "保存项目最近一次最后日志", description = "只需要 projectId；可选 pipelineId 限定某条流水线。")
    public PipelineRunLogResponse captureLatest(@Valid @RequestBody LatestProjectPipelineRunLogRequest request) {
        PipelineRun pipelineRun = latestPipelineRunFinder.find(responseMapper.toCommand(request));
        return responseMapper.toResponse(capturePipelineRunLogService.capture(pipelineRun));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "实时查看 Jenkins 构建日志", description = "SSE 流式返回新增日志片段；这个接口不落库。")
    public SseEmitter stream(@Valid @RequestBody PipelineRunLogRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        CompletableFuture.runAsync(() -> streamLog(request, emitter));
        return emitter;
    }

    @PostMapping(value = "/stream-latest", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "实时查看项目最近一次构建日志", description = "只需要 projectId；可选 pipelineId 限定某条流水线。")
    public SseEmitter streamLatest(@Valid @RequestBody LatestProjectPipelineRunLogRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        CompletableFuture.runAsync(() -> streamLatestLog(request, emitter));
        return emitter;
    }

    private void streamLog(PipelineRunLogRequest request, SseEmitter emitter) {
        try {
            PipelineRun pipelineRun = loadPipelineRun(responseMapper.toCommand(request));
            streamPipelineRunLog(pipelineRun, emitter);
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void streamLatestLog(LatestProjectPipelineRunLogRequest request, SseEmitter emitter) {
        try {
            PipelineRun pipelineRun = latestPipelineRunFinder.find(responseMapper.toCommand(request));
            streamPipelineRunLog(pipelineRun, emitter);
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void streamPipelineRunLog(PipelineRun pipelineRun, SseEmitter emitter) {
        try {
            int sentLength = 0;
            for (int index = 0; index < STREAM_POLL_LIMIT; index++) {
                JenkinsConsoleLog consoleLog = pipelineRunLogFetcher.fetch(pipelineRun);
                String text = consoleLog.text();
                if (text.length() > sentLength) {
                    emitter.send(SseEmitter.event()
                            .name("log")
                            .data(text.substring(sentLength)));
                    sentLength = text.length();
                }
                if (pipelineRun.isTerminal()) {
                    break;
                }
                Thread.sleep(STREAM_POLL_INTERVAL_MILLIS);
                pipelineRun = loadPipelineRun(new PipelineRunLogCommand(pipelineRun.id()));
            }
            emitter.send(SseEmitter.event().name("done").data("completed"));
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            emitter.completeWithError(exception);
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }

    private PipelineRun loadPipelineRun(PipelineRunLogCommand command) {
        return pipelineRunRepository.findById(command.pipelineRunId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline run not found"
                ));
    }
}
