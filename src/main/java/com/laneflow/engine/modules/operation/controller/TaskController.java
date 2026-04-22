package com.laneflow.engine.modules.operation.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.operation.response.TaskResponse;
import com.laneflow.engine.modules.operation.service.TaskService;
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

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
