package com.laneflow.engine.modules.workflow.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Swimlane {

    private String id;
    private String name;
    private String departmentId;
    private String departmentCode;
}
