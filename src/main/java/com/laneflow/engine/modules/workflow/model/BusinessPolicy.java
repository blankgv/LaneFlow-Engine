package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.embedded.PolicyAction;
import com.laneflow.engine.modules.workflow.model.embedded.PolicyCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "business_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessPolicy {

    @Id
    private String id;

    private String name;
    private String description;

    @Indexed
    private String workflowDefinitionId;

    private String nodeId;

    @Builder.Default
    private boolean active = true;

    private int priority;

    private List<PolicyCondition> conditions;
    private List<PolicyAction> actions;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
