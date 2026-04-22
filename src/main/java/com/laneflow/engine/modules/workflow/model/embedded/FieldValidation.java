package com.laneflow.engine.modules.workflow.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidation {

    private String type;
    private String value;
    private String message;
}
