package com.laneflow.engine.modules.operation.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.operation.request.ApplicantRequest;
import com.laneflow.engine.modules.operation.response.ApplicantResponse;
import com.laneflow.engine.modules.operation.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService service;

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ApplicantResponse> create(@Valid @RequestBody ApplicantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<ApplicantResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<ApplicantResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<ApplicantResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody ApplicantRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
