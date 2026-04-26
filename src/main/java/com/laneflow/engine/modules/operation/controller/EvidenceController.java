package com.laneflow.engine.modules.operation.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.operation.model.enums.EvidenceCategory;
import com.laneflow.engine.modules.operation.response.EvidenceResponse;
import com.laneflow.engine.modules.operation.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/evidences")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<EvidenceResponse> upload(@RequestParam String procedureId,
                                                    @RequestParam(required = false) String taskId,
                                                    @RequestParam String nodeId,
                                                    @RequestParam String fieldName,
                                                    @RequestParam(required = false) String description,
                                                    @RequestParam(required = false) EvidenceCategory category,
                                                    @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.upload(
                        procedureId,
                        taskId,
                        nodeId,
                        fieldName,
                        description,
                        category,
                        file,
                        currentUsername()
                ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<EvidenceResponse>> findByProcedure(@RequestParam String procedureId) {
        return ResponseEntity.ok(service.findByProcedure(procedureId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<EvidenceResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_WRITE + "')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
