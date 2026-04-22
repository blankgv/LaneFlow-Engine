package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.BusinessPolicy;
import com.laneflow.engine.modules.workflow.model.embedded.PolicyAction;
import com.laneflow.engine.modules.workflow.model.embedded.PolicyCondition;
import com.laneflow.engine.modules.workflow.model.enums.LogicalOperator;
import com.laneflow.engine.modules.workflow.repository.BusinessPolicyRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.request.CreatePolicyRequest;
import com.laneflow.engine.modules.workflow.request.UpdatePolicyRequest;
import com.laneflow.engine.modules.workflow.response.BusinessPolicyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessPolicyServiceImpl implements BusinessPolicyService {

    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    @Override
    public List<BusinessPolicyResponse> findByWorkflow(String workflowId) {
        return businessPolicyRepository.findByWorkflowDefinitionId(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BusinessPolicyResponse findById(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));
        return toResponse(policy);
    }

    @Override
    public BusinessPolicyResponse create(CreatePolicyRequest request) {
        if (!workflowDefinitionRepository.existsById(request.workflowDefinitionId())) {
            throw new IllegalArgumentException("Workflow no encontrado: " + request.workflowDefinitionId());
        }

        List<PolicyCondition> conditions = request.conditions().stream()
                .map(c -> PolicyCondition.builder()
                        .field(c.field())
                        .operator(c.operator())
                        .value(c.value())
                        .logicalOperator(c.logicalOperator() != null ? c.logicalOperator() : LogicalOperator.AND)
                        .build())
                .toList();

        List<PolicyAction> actions = request.actions().stream()
                .map(a -> PolicyAction.builder()
                        .type(a.type())
                        .targetNodeId(a.targetNodeId())
                        .targetNodeName(a.targetNodeName())
                        .targetDepartmentId(a.targetDepartmentId())
                        .variableName(a.variableName())
                        .variableValue(a.variableValue())
                        .notificationMessage(a.notificationMessage())
                        .build())
                .toList();

        BusinessPolicy policy = BusinessPolicy.builder()
                .name(request.name())
                .description(request.description())
                .workflowDefinitionId(request.workflowDefinitionId())
                .nodeId(request.nodeId())
                .active(true)
                .priority(request.priority())
                .conditions(conditions)
                .actions(actions)
                .build();

        BusinessPolicy saved = businessPolicyRepository.save(policy);
        log.info("Política creada: {} para workflow {}", saved.getName(), saved.getWorkflowDefinitionId());
        return toResponse(saved);
    }

    @Override
    public BusinessPolicyResponse update(String id, UpdatePolicyRequest request) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));

        if (request.name() != null) policy.setName(request.name());
        if (request.description() != null) policy.setDescription(request.description());
        if (request.nodeId() != null) policy.setNodeId(request.nodeId());
        if (request.priority() != null) policy.setPriority(request.priority());

        if (request.conditions() != null) {
            policy.setConditions(request.conditions().stream()
                    .map(c -> PolicyCondition.builder()
                            .field(c.field())
                            .operator(c.operator())
                            .value(c.value())
                            .logicalOperator(c.logicalOperator() != null ? c.logicalOperator() : LogicalOperator.AND)
                            .build())
                    .toList());
        }

        if (request.actions() != null) {
            policy.setActions(request.actions().stream()
                    .map(a -> PolicyAction.builder()
                            .type(a.type())
                            .targetNodeId(a.targetNodeId())
                            .targetNodeName(a.targetNodeName())
                            .targetDepartmentId(a.targetDepartmentId())
                            .variableName(a.variableName())
                            .variableValue(a.variableValue())
                            .notificationMessage(a.notificationMessage())
                            .build())
                    .toList());
        }

        policy.setUpdatedAt(LocalDateTime.now());
        return toResponse(businessPolicyRepository.save(policy));
    }

    @Override
    public BusinessPolicyResponse toggleActive(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));

        policy.setActive(!policy.isActive());
        policy.setUpdatedAt(LocalDateTime.now());

        log.info("Política {} cambiada a active={}", policy.getName(), policy.isActive());
        return toResponse(businessPolicyRepository.save(policy));
    }

    @Override
    public void delete(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));
        businessPolicyRepository.delete(policy);
        log.info("Política eliminada: {}", policy.getName());
    }

    private BusinessPolicyResponse toResponse(BusinessPolicy p) {
        return new BusinessPolicyResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getWorkflowDefinitionId(),
                p.getNodeId(),
                p.isActive(),
                p.getPriority(),
                p.getConditions(),
                p.getActions(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
