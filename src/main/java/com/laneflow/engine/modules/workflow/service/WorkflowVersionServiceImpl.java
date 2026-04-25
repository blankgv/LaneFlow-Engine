package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.WorkflowVersion;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowVersionStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowVersionRepository;
import com.laneflow.engine.modules.workflow.request.CreateVersionRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowVersionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowVersionServiceImpl implements WorkflowVersionService {

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final RepositoryService repositoryService;

    @Override
    public List<WorkflowVersionResponse> findByWorkflow(String workflowId) {
        return workflowVersionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WorkflowVersionResponse findByWorkflowAndVersion(String workflowId, int versionNumber) {
        WorkflowVersion version = workflowVersionRepository
                .findByWorkflowDefinitionIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Versión " + versionNumber + " no encontrada para el workflow: " + workflowId));
        return toResponse(version);
    }

    @Override
    public WorkflowVersionResponse createDraft(String workflowId, CreateVersionRequest request, String createdBy) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));

        List<WorkflowVersion> existing = workflowVersionRepository
                .findByWorkflowDefinitionIdOrderByVersionNumberDesc(workflowId);

        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        WorkflowVersion version = WorkflowVersion.builder()
                .workflowDefinitionId(workflowId)
                .versionNumber(nextVersion)
                .bpmnXml(request.bpmnXml())
                .status(WorkflowVersionStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        if (request.bpmnXml() != null && !request.bpmnXml().isBlank()) {
            wf.setDraftBpmnXml(request.bpmnXml().trim());
            wf.setLastModifiedBy(createdBy);
            wf.setUpdatedAt(LocalDateTime.now());
            workflowDefinitionRepository.save(wf);
        }

        WorkflowVersion saved = workflowVersionRepository.save(version);
        log.info("Version {} DRAFT creada para workflow {}", nextVersion, wf.getCode());
        return toResponse(saved);
    }

    @Override
    public WorkflowVersionResponse publish(String workflowId, int versionNumber, String publishedBy) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));

        WorkflowVersion version = workflowVersionRepository
                .findByWorkflowDefinitionIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Versión " + versionNumber + " no encontrada para el workflow: " + workflowId));

        if (version.getStatus() == WorkflowVersionStatus.PUBLISHED) {
            throw new IllegalStateException("La versión ya está publicada.");
        }

        if (version.getBpmnXml() == null || version.getBpmnXml().isBlank()) {
            throw new IllegalStateException("La versión no tiene BPMN XML para publicar.");
        }

        // Deprecate any existing PUBLISHED version
        workflowVersionRepository.findByWorkflowDefinitionIdAndStatus(workflowId, WorkflowVersionStatus.PUBLISHED)
                .ifPresent(prev -> {
                    prev.setStatus(WorkflowVersionStatus.DEPRECATED);
                    workflowVersionRepository.save(prev);
                });

        String bpmnXml = BpmnDeploymentPreparer.prepareForDeployment(
                version.getBpmnXml(),
                wf.getCamundaProcessKey(),
                wf.getName()
        );

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(wf.getCamundaProcessKey() + "_v" + versionNumber + ".bpmn",
                            new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                    .name(wf.getName() + " v" + versionNumber)
                    .deploy();

            String processDefinitionId = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult()
                    .getId();

            version.setCamundaDeploymentId(deployment.getId());
            version.setCamundaProcessDefinitionId(processDefinitionId);
            version.setBpmnXml(bpmnXml);

            wf.setCamundaDeploymentId(deployment.getId());
            wf.setCamundaProcessDefinitionId(processDefinitionId);
            wf.setCurrentVersion(versionNumber);
            wf.setPublishedVersionNumber(versionNumber);
            wf.setStatus(WorkflowStatus.PUBLISHED);
            wf.setDraftBpmnXml(bpmnXml);
            wf.setLastModifiedBy(publishedBy);
            wf.setPublishedAt(LocalDateTime.now());
            wf.setUpdatedAt(LocalDateTime.now());

            log.info("Version {} del workflow {} desplegada en Camunda. DeploymentId: {}",
                    versionNumber, wf.getCode(), deployment.getId());
        } catch (Exception e) {
            log.error("Error al desplegar versión {} del workflow {}: {}", versionNumber, wf.getCode(), e.getMessage());
            throw new IllegalStateException("Error al desplegar en Camunda: " + e.getMessage(), e);
        }

        version.setStatus(WorkflowVersionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        workflowDefinitionRepository.save(wf);

        return toResponse(workflowVersionRepository.save(version));
    }

    private WorkflowVersionResponse toResponse(WorkflowVersion v) {
        return new WorkflowVersionResponse(
                v.getId(),
                v.getWorkflowDefinitionId(),
                v.getVersionNumber(),
                v.getBpmnXml(),
                v.getStatus(),
                v.getCamundaDeploymentId(),
                v.getCreatedAt(),
                v.getPublishedAt()
        );
    }
}
