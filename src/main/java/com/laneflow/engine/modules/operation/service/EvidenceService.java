package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.model.enums.EvidenceCategory;
import com.laneflow.engine.modules.operation.response.EvidenceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EvidenceService {
    EvidenceResponse upload(String procedureId,
                            String taskId,
                            String nodeId,
                            String fieldName,
                            String description,
                            EvidenceCategory category,
                            MultipartFile file,
                            String uploadedBy);

    List<EvidenceResponse> findByProcedure(String procedureId);
    EvidenceResponse findById(String id);
    void delete(String id);
}
