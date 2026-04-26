package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaboratorResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowEditorResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowEditorTaskResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInvitationResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowEditorServiceImpl implements WorkflowEditorService {

    private final WorkflowAccessService workflowAccessService;
    private final WorkflowService workflowService;
    private final DynamicFormService dynamicFormService;
    private final WorkflowCollaborationService workflowCollaborationService;

    @Override
    public WorkflowEditorResponse getEditorSnapshot(String workflowId, String username) {
        WorkflowDefinition workflow = workflowAccessService.requireReadable(workflowId, username);
        WorkflowResponse workflowResponse = workflowService.findById(workflowId, username);
        List<DynamicFormResponse> forms = dynamicFormService.findByWorkflow(workflowId, username);
        List<WorkflowCollaboratorResponse> collaborators =
                workflowCollaborationService.findCollaborators(workflowId, username);
        List<WorkflowInvitationResponse> invitations =
                workflowCollaborationService.findInvitationsByWorkflow(workflowId, username);

        Map<String, DynamicFormResponse> formsByNode = forms.stream()
                .collect(Collectors.toMap(DynamicFormResponse::nodeId, Function.identity()));
        Map<String, Swimlane> swimlanesById = workflow.getSwimlanes() == null
                ? Collections.emptyMap()
                : workflow.getSwimlanes().stream()
                .collect(Collectors.toMap(Swimlane::getId, Function.identity(), (left, right) -> left));

        List<WorkflowEditorTaskResponse> tasks = workflow.getNodes() == null
                ? List.of()
                : workflow.getNodes().stream()
                .filter(node -> node.getType() == NodeType.USER_TASK)
                .map(node -> toTaskResponse(node, swimlanesById.get(node.getSwimlaneId()), formsByNode.get(node.getId())))
                .toList();

        return new WorkflowEditorResponse(
                workflowResponse,
                workflowAccessService.canWrite(workflow, username),
                tasks,
                forms,
                collaborators,
                invitations
        );
    }

    private WorkflowEditorTaskResponse toTaskResponse(
            WorkflowNode node,
            Swimlane swimlane,
            DynamicFormResponse form
    ) {
        return new WorkflowEditorTaskResponse(
                node.getId(),
                node.getName(),
                node.getSwimlaneId(),
                node.getDepartmentId(),
                swimlane == null ? null : swimlane.getDepartmentCode(),
                swimlane == null ? null : swimlane.getName(),
                node.getRequiredAction(),
                form == null ? null : form.id(),
                form == null ? null : form.title()
        );
    }
}
