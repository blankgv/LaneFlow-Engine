package com.laneflow.engine.modules.operation.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.operation.request.ApproveTaskRequest;
import com.laneflow.engine.modules.operation.request.CompleteTaskRequest;
import com.laneflow.engine.modules.operation.request.ObserveTaskRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.operation.response.TaskResponse;
import com.laneflow.engine.modules.operation.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<TaskResponse>> getAvailable() {
        return ResponseEntity.ok(service.getAvailable(currentUsername()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<TaskResponse>> getMine() {
        return ResponseEntity.ok(service.getMine(currentUsername()));
    }

    @PostMapping("/{taskId}/claim")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<TaskResponse> claim(@PathVariable String taskId) {
        return ResponseEntity.ok(service.claim(taskId, currentUsername()));
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ProcedureResponse> complete(@PathVariable String taskId,
                                                       @Valid @RequestBody CompleteTaskRequest request) {
        return ResponseEntity.ok(service.complete(taskId, request, currentUsername()));
    }

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ProcedureResponse> approve(@PathVariable String taskId,
                                                      @Valid @RequestBody ApproveTaskRequest request) {
        return ResponseEntity.ok(service.approve(taskId, request, currentUsername()));
    }

    @PostMapping("/{taskId}/observe")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ProcedureResponse> observe(@PathVariable String taskId,
                                                      @Valid @RequestBody ObserveTaskRequest request) {
        return ResponseEntity.ok(service.observe(taskId, request, currentUsername()));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
