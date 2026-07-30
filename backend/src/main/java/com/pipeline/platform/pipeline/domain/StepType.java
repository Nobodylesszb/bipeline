package com.pipeline.platform.pipeline.domain;

public enum StepType {
    CHECKOUT,
    SHELL,
    TEST,
    SONAR_SCAN,
    BUILD_IMAGE,
    PUSH_IMAGE
}
