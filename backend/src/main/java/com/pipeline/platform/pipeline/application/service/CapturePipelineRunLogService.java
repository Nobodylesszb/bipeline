package com.pipeline.platform.pipeline.application.service;

import com.pipeline.platform.jenkins.application.model.JenkinsConsoleLog;
import com.pipeline.platform.pipeline.application.command.PipelineRunLogCommand;
import com.pipeline.platform.pipeline.application.model.PipelineRunLogView;
import com.pipeline.platform.pipeline.domain.PipelineRun;
import com.pipeline.platform.pipeline.domain.PipelineRunLog;
import com.pipeline.platform.pipeline.domain.PipelineRunLogRepository;
import com.pipeline.platform.pipeline.domain.PipelineRunRepository;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CapturePipelineRunLogService {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunLogRepository pipelineRunLogRepository;

    @Autowired
    private PipelineRunLogFetcher pipelineRunLogFetcher;

    @Autowired
    private ClockProvider clockProvider;

    @Transactional
    public PipelineRunLogView capture(PipelineRunLogCommand command) {
        PipelineRun pipelineRun = pipelineRunRepository.findById(command.pipelineRunId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Pipeline run not found"
                ));
        return capture(pipelineRun);
    }

    public PipelineRunLogView capture(PipelineRun pipelineRun) {
        JenkinsConsoleLog consoleLog = pipelineRunLogFetcher.fetch(pipelineRun);
        PipelineRunLog savedLog = pipelineRunLogRepository.findLatestByPipelineRunId(pipelineRun.id())
                .map(existingLog -> existingLog.replaceWith(
                        consoleLog.externalUrl(),
                        consoleLog.text(),
                        clockProvider.now()
                ))
                .orElseGet(() -> PipelineRunLog.capture(
                        pipelineRun.id(),
                        consoleLog.externalUrl(),
                        consoleLog.text(),
                        clockProvider.now()
                ));
        return PipelineRunLogView.from(pipelineRunLogRepository.save(savedLog));
    }
}
