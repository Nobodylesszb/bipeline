package com.pipeline.platform.shared.id;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    public UUID nextId() {
        return UUID.randomUUID();
    }
}
