package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.WorkflowVersion;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
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
    private final BpmnMetadataExtractor bpmnMetadataExtractor;
    private final WorkflowModelValidator workflowModelValidator;
    private final WorkflowAuditService workflowAuditService;
    private final DynamicFormService dynamicFormService;
    private final WorkflowAccessService workflowAccessService;

    @Override
    public List<WorkflowVersionResponse> findByWorkflow(String workflowId, String username) {
        workflowAccessService.requireReadable(workflowId, username);
        return workflowVersionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WorkflowVersionResponse findByWorkflowAndVersion(String workflowId, int versionNumber, String username) {
        workflowAccessService.requireReadable(workflowId, username);
        WorkflowVersion version = workflowVersionRepository
                .findByWorkflowDefinitionIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + versionNumber + " no encontrada para el workflow: " + workflowId));
        return toResponse(version);
    }

    @Override
    public WorkflowVersionResponse createDraft(String workflowId, CreateVersionRequest request, String createdBy) {
        WorkflowDefinition wf = workflowAccessService.requireWritable(workflowId, createdBy);

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
            BpmnMetadataExtractor.BpmnStructure structure = bpmnMetadataExtractor.extract(wf.getDraftBpmnXml());
            workflowModelValidator.validateDraft(wf.getDraftBpmnXml(), structure.swimlanes(), structure.nodes(), structure.transitions());
            dynamicFormService.validateNodeBindings(wf.getId(), structure.nodes());
            wf.setSwimlanes(structure.swimlanes());
            wf.setNodes(structure.nodes());
            wf.setTransitions(structure.transitions());
            dynamicFormService.syncNodeBindings(wf.getId(), wf.getNodes());
            wf.setLastModifiedBy(createdBy);
            wf.setUpdatedAt(LocalDateTime.now());
            workflowDefinitionRepository.save(wf);
        }

        WorkflowVersion saved = workflowVersionRepository.save(version);
        workflowAuditService.record(
                wf,
                WorkflowAuditAction.VERSION_DRAFT_CREATED,
                "Creacion de una nueva version borrador.",
                createdBy,
                wf.getStatus(),
                wf.getStatus(),
                java.util.Map.of(
                        "workflowCode", wf.getCode(),
                        "workflowName", wf.getName(),
                        "versionNumber", nextVersion
                )
        );
        log.info("Version {} DRAFT creada para workflow {}", nextVersion, wf.getCode());
        return toResponse(saved);
    }

    @Override
    public WorkflowVersionResponse publish(String workflowId, int versionNumber, String publishedBy) {
        WorkflowDefinition wf = workflowAccessService.requireWritable(workflowId, publishedBy);
        WorkflowStatus statusBefore = wf.getStatus();

        WorkflowVersion version = workflowVersionRepository
                .findByWorkflowDefinitionIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + versionNumber + " no encontrada para el workflow: " + workflowId));

        if (version.getStatus() == WorkflowVersionStatus.PUBLISHED) {
            throw new IllegalStateException("La version ya esta publicada.");
        }

        if (version.getBpmnXml() == null || version.getBpmnXml().isBlank()) {
            throw new IllegalStateException("La version no tiene BPMN XML para publicar.");
        }

        dynamicFormService.validateNodeBindings(wf.getId(), wf.getNodes());

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
        BpmnMetadataExtractor.BpmnStructure publishStructure = bpmnMetadataExtractor.extract(bpmnXml);
        workflowModelValidator.validatePublishable(
                wf.getCode(),
                wf.getName(),
                publishStructure.swimlanes(),
                publishStructure.nodes(),
                publishStructure.transitions(),
                publishStructure.participantCount(),
                publishStructure.laneCount()
        );
        wf.setSwimlanes(publishStructure.swimlanes());
        wf.setNodes(publishStructure.nodes());
        wf.setTransitions(publishStructure.transitions());
        dynamicFormService.syncNodeBindings(wf.getId(), wf.getNodes());

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(wf.getCamundaProcessKey() + "_v" + versionNumber + ".bpmn",
                            new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                    .name(wf.getName() + " v" + versionNumber)
                    .deploy();

            String processDefinitionId = CamundaDeploymentSupport
                    .resolveProcessDefinition(repositoryService, deployment, wf.getCamundaProcessKey())
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
            log.error("Error al desplegar version {} del workflow {}: {}", versionNumber, wf.getCode(), e.getMessage(), e);
            throw new IllegalStateException("Error al desplegar en Camunda: " + e.getMessage(), e);
        }

        version.setStatus(WorkflowVersionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        WorkflowDefinition savedWorkflow = workflowDefinitionRepository.save(wf);
        workflowAuditService.record(
                savedWorkflow,
                WorkflowAuditAction.VERSION_PUBLISHED,
                "Publicacion de version desde el historial de versiones.",
                publishedBy,
                statusBefore,
                savedWorkflow.getStatus(),
                java.util.Map.of(
                        "workflowCode", savedWorkflow.getCode(),
                        "workflowName", savedWorkflow.getName(),
                        "versionNumber", versionNumber,
                        "camundaDeploymentId", version.getCamundaDeploymentId(),
                        "camundaProcessDefinitionId", version.getCamundaProcessDefinitionId()
                )
        );

        return toResponse(workflowVersionRepository.save(version));
    }

    private WorkflowVersionResponse toResponse(WorkflowVersion version) {
        return new WorkflowVersionResponse(
                version.getId(),
                version.getWorkflowDefinitionId(),
                version.getVersionNumber(),
                version.getBpmnXml(),
                version.getStatus(),
                version.getCamundaDeploymentId(),
                version.getCreatedAt(),
                version.getPublishedAt()
        );
    }
}
