package com.pipeline.platform.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

class SuccessResponseAdviceTest {

    private final SuccessResponseAdvice advice = new SuccessResponseAdvice();

    @Test
    void wrapsSuccessfulBodyWithSuccessCode() {
        Object response = advice.beforeBodyWrite(
                Map.of("id", 1L),
                null,
                MediaType.APPLICATION_JSON,
                JacksonJsonHttpMessageConverter.class,
                null,
                null
        );

        assertThat(response).isInstanceOf(SuccessResponse.class);
        SuccessResponse<?> successResponse = (SuccessResponse<?>) response;
        assertThat(successResponse.code()).isEqualTo(200);
        assertThat(successResponse.data()).isEqualTo(Map.of("id", 1L));
    }

    @Test
    void keepsErrorResponseUnwrapped() {
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                "Request validation failed",
                Map.of(),
                "trace-1"
        );

        Object response = advice.beforeBodyWrite(
                errorResponse,
                null,
                MediaType.APPLICATION_JSON,
                JacksonJsonHttpMessageConverter.class,
                null,
                null
        );

        assertThat(response).isSameAs(errorResponse);
    }
}
