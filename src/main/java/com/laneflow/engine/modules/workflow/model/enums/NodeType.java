package com.laneflow.engine.modules.workflow.model.enums;

public enum NodeType {
    START_EVENT,
    END_EVENT,
    INTERMEDIATE_EVENT,
    USER_TASK,
    SERVICE_TASK,
    EXCLUSIVE_GATEWAY,
    PARALLEL_GATEWAY,
    INCLUSIVE_GATEWAY
}
