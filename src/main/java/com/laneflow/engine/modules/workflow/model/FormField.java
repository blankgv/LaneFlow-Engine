package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.embedded.FieldValidation;
import com.laneflow.engine.modules.workflow.model.embedded.FileConfig;
import com.laneflow.engine.modules.workflow.model.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "form_fields")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormField {

    @Id
    private String id;

    @Indexed
    private String formId;

    private String name;
    private String label;
    private FieldType type;
    private boolean required;
    private int order;

    private List<String> options;
    private List<FieldValidation> validations;
    private FileConfig fileConfig;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
