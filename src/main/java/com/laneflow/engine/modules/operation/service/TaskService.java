package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.response.TaskResponse;

import java.util.List;

public interface TaskService {
    List<TaskResponse> getAvailable(String username);
    List<TaskResponse> getMine(String username);
    TaskResponse claim(String taskId, String username);
}
