package com.pipeline.platform.pipeline.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pipeline.platform.pipeline.application.command.ChangePipelineStatusCommand;
import com.pipeline.platform.pipeline.application.model.PipelineView;
import com.pipeline.platform.pipeline.domain.Pipeline;
import com.pipeline.platform.pipeline.domain.PipelineRepository;
import com.pipeline.platform.pipeline.domain.PipelineStage;
import com.pipeline.platform.pipeline.domain.PipelineStatus;
import com.pipeline.platform.pipeline.domain.TriggerType;
import com.pipeline.platform.shared.error.BusinessException;
import com.pipeline.platform.shared.error.ErrorCode;
import com.pipeline.platform.shared.time.ClockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChangePipelineStatusServiceTest {

    private final ClockProvider clockProvider = new ClockProvider(
            Clock.fixed(Instant.parse("2026-07-29T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void activatesDraftPipeline() {
        InMemoryPipelineRepository repository = new InMemoryPipelineRepository(draftPipeline());
        ChangePipelineStatusService service = service(repository);

        PipelineView result = service.activate(new ChangePipelineStatusCommand(1L));

        assertThat(result.status()).isEqualTo(PipelineStatus.ACTIVE);
    }

    @Test
    void disablesActivePipeline() {
        InMemoryPipelineRepository repository = new InMemoryPipelineRepository(activePipeline());
        ChangePipelineStatusService service = service(repository);

        PipelineView result = service.disable(new ChangePipelineStatusCommand(1L));

        assertThat(result.status()).isEqualTo(PipelineStatus.DISABLED);
    }

    @Test
    void rejectsDisablingDraftPipeline() {
        ChangePipelineStatusService service = service(new InMemoryPipelineRepository(draftPipeline()));

        assertThatThrownBy(() -> service.disable(new ChangePipelineStatusCommand(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    private ChangePipelineStatusService service(PipelineRepository repository) {
        ChangePipelineStatusService service = new ChangePipelineStatusService();
        ReflectionTestUtils.setField(service, "pipelineRepository", repository);
        ReflectionTestUtils.setField(service, "clockProvider", clockProvider);
        return service;
    }

    private Pipeline draftPipeline() {
        return pipeline(PipelineStatus.DRAFT);
    }

    private Pipeline activePipeline() {
        return pipeline(PipelineStatus.ACTIVE);
    }

    private Pipeline pipeline(PipelineStatus status) {
        return new Pipeline(
                1L,
                1L,
                "main-ci",
                "主分支 CI",
                status,
                TriggerType.MANUAL,
                "master",
                1,
                List.of(new PipelineStage(
                        1L,
                        1L,
                        "default",
                        "默认阶段",
                        1,
                        List.of(),
                        clockProvider.now(),
                        clockProvider.now()
                )),
                clockProvider.now(),
                clockProvider.now()
        );
    }

    private static class InMemoryPipelineRepository implements PipelineRepository {

        private final List<Pipeline> pipelines = new ArrayList<>();

        private InMemoryPipelineRepository(Pipeline pipeline) {
            pipelines.add(pipeline);
        }

        @Override
        public boolean existsByProjectIdAndName(Long projectId, String name) {
            return pipelines.stream()
                    .anyMatch(pipeline -> pipeline.projectId().equals(projectId)
                            && pipeline.name().equals(name));
        }

        @Override
        public Pipeline save(Pipeline pipeline) {
            pipelines.removeIf(existing -> existing.id().equals(pipeline.id()));
            pipelines.add(pipeline);
            return pipeline;
        }

        @Override
        public Optional<Pipeline> findById(Long id) {
            return pipelines.stream()
                    .filter(pipeline -> pipeline.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<Pipeline> findByProjectId(Long projectId) {
            return pipelines.stream()
                    .filter(pipeline -> pipeline.projectId().equals(projectId))
                    .toList();
        }
    }
}
