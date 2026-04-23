package com.laneflow.engine.modules.operation.model;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "procedures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    @Indexed
    private String workflowDefinitionId;

    private String workflowCode;
    private String workflowName;
    private int workflowVersion;
    private String camundaProcessKey;
    private String camundaProcessInstanceId;
    private String previousCamundaProcessInstanceId;

    @Indexed
    private String applicantId;

    private String applicantDocumentNumber;
    private String applicantName;

    private ProcedureStatus status;
    private String currentTaskId;
    private String currentNodeId;
    private String currentNodeName;
    private String currentAssigneeUsername;
    private LocalDateTime claimedAt;
    private Map<String, Object> formData;
    private String lastAction;
    private String lastComment;
    private String lastCompletedTaskId;
    private String lastCompletedNodeId;
    private String lastCompletedTaskName;
    private String lastCompletedBy;
    private LocalDateTime lastCompletedAt;
    private int resubmissionCount;
    private String resolvedObservationBy;
    private LocalDateTime resolvedObservationAt;
    private String resolvedObservationComment;
    private String startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
