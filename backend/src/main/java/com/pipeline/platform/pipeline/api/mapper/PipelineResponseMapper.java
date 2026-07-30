package com.pipeline.platform.pipeline.api.mapper;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.platform.pipeline.api.request.CreatePipelineRequest;
import com.pipeline.platform.pipeline.api.response.PipelineResponse;
import com.pipeline.platform.pipeline.api.response.PipelineStageResponse;
import com.pipeline.platform.pipeline.api.response.PipelineStepResponse;
import com.pipeline.platform.pipeline.application.command.CreatePipelineCommand;
import com.pipeline.platform.pipeline.application.model.PipelineStageView;
import com.pipeline.platform.pipeline.application.model.PipelineStepView;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PipelineResponseMapper {

    private static final TypeReference<Map<String, Object>> CONFIG_TYPE = new TypeReference<>() {
    };

    @Autowired
    private ObjectMapper objectMapper;

    public CreatePipelineCommand toCommand(CreatePipelineRequest request) {
        return new CreatePipelineCommand(
                request.projectId(),
                request.name(),
                request.description(),
                request.triggerType(),
                request.branchName(),
                request.stages().stream()
                        .map(this::toStageCommand)
                        .toList()
        );
    }

    public PipelineResponse toResponse(PipelineView view) {
        return new PipelineResponse(
                view.id(),
                view.projectId(),
                view.name(),
                view.description(),
                view.status(),
                view.triggerType(),
                view.branchName(),
                view.version(),
                view.stages().stream()
                        .map(this::toStageResponse)
                        .toList(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private CreatePipelineCommand.StageCommand toStageCommand(CreatePipelineRequest.StageRequest request) {
        return new CreatePipelineCommand.StageCommand(
                request.name(),
                request.displayName(),
                request.steps().stream()
                        .map(this::toStepCommand)
                        .toList()
        );
    }

    private CreatePipelineCommand.StepCommand toStepCommand(CreatePipelineRequest.StepRequest request) {
        return new CreatePipelineCommand.StepCommand(
                request.type(),
                request.name(),
                request.displayName(),
                toConfigJson(request.config())
        );
    }

    private PipelineStageResponse toStageResponse(PipelineStageView view) {
        return new PipelineStageResponse(
                view.id(),
                view.name(),
                view.displayName(),
                view.sortOrder(),
                view.steps().stream()
                        .map(this::toStepResponse)
                        .toList()
        );
    }

    private PipelineStepResponse toStepResponse(PipelineStepView view) {
        return new PipelineStepResponse(
                view.id(),
                view.name(),
                view.displayName(),
                view.type(),
                view.sortOrder(),
                toConfigObject(view.configJson()),
                view.enabled()
        );
    }

    private String toConfigJson(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config == null ? Map.of() : config);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Step config must be valid JSON object"
            );
        }
    }

    private Object toConfigObject(String configJson) {
        try {
            return objectMapper.readValue(configJson, CONFIG_TYPE);
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }
}
