package com.laneflow.engine.modules.operation.model;

import com.laneflow.engine.modules.operation.model.enums.EvidenceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "evidences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    @Id
    private String id;

    @Indexed
    private String procedureId;

    private String taskId;
    private String nodeId;
    private String formId;
    private String fieldId;
    private String fieldName;
    private String uploadedBy;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private String extension;
    private long sizeBytes;
    private String storageProvider;
    private String bucketName;
    private String storagePath;
    private String mediaLink;
    private String description;
    private EvidenceCategory category;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
