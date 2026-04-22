package com.laneflow.engine.modules.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "dynamic_forms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicForm {

    @Id
    private String id;

    @Indexed
    private String workflowDefinitionId;

    private String nodeId;
    private String nodeName;
    private String title;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
