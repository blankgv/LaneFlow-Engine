package com.laneflow.engine.modules.tracking.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.tracking.response.ProcedureHistoryResponse;
import com.laneflow.engine.modules.tracking.response.ProcedureNotificationResponse;
import com.laneflow.engine.modules.tracking.response.ProcedureStatusResponse;
import com.laneflow.engine.modules.tracking.service.ProcedureAuditService;
import com.laneflow.engine.modules.tracking.service.ProcedureNotificationService;
import com.laneflow.engine.modules.tracking.service.ProcedureTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/tracking/procedures")
@RequiredArgsConstructor
public class ProcedureTrackingController {

    private final ProcedureAuditService procedureAuditService;
    private final ProcedureTrackingService procedureTrackingService;
    private final ProcedureNotificationService procedureNotificationService;

    @GetMapping("/{procedureId}/status")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<ProcedureStatusResponse> getStatus(@PathVariable String procedureId) {
        return ResponseEntity.ok(procedureTrackingService.getStatus(procedureId));
    }

    @GetMapping("/{procedureId}/history")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<ProcedureHistoryResponse> getHistory(@PathVariable String procedureId) {
        return ResponseEntity.ok(procedureAuditService.getHistory(procedureId));
    }

    @GetMapping("/{procedureId}/notifications")
    @PreAuthorize("hasAuthority('" + Permission.TRAMITE_READ + "')")
    public ResponseEntity<List<ProcedureNotificationResponse>> getNotifications(@PathVariable String procedureId) {
        return ResponseEntity.ok(procedureNotificationService.getByProcedure(procedureId));
    }
}
