package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.request.ApproveTaskRequest;
import com.laneflow.engine.modules.operation.request.CompleteTaskRequest;
import com.laneflow.engine.modules.operation.request.ObserveTaskRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.operation.response.TaskResponse;

import java.util.List;

public interface TaskService {
    List<TaskResponse> getAvailable(String username);
    List<TaskResponse> getMine(String username);
    TaskResponse claim(String taskId, String username);
    ProcedureResponse complete(String taskId, CompleteTaskRequest request, String username);
    ProcedureResponse approve(String taskId, ApproveTaskRequest request, String username);
    ProcedureResponse observe(String taskId, ObserveTaskRequest request, String username);
}
