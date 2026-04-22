package com.laneflow.engine.modules.operation.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.operation.request.StartProcedureRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.operation.service.ProcedureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService service;

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ProcedureResponse> start(@Valid @RequestBody StartProcedureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.start(request, currentUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<ProcedureResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<ProcedureResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
