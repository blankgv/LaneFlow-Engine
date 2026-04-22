package com.laneflow.engine.modules.workflow.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileConfig {

    private List<String> allowedExtensions;
    private int maxSizeMb;
    private boolean multiple;
    private String bucketFolder;
}
