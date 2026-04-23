package com.laneflow.engine.modules.tracking.model;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "procedure_audits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureAudit {

    @Id
    private String id;

    @Indexed
    private String procedureId;

    private String procedureCode;
    private ProcedureAuditAction action;
    private String description;
    private String username;
    private String taskId;
    private String nodeId;
    private String nodeName;
    private ProcedureStatus statusBefore;
    private ProcedureStatus statusAfter;
    private Map<String, Object> metadata;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
