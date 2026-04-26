package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.DynamicForm;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import com.laneflow.engine.modules.workflow.repository.DynamicFormRepository;
import com.laneflow.engine.modules.workflow.repository.FormFieldRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicFormServiceImplTest {

    private final DynamicFormRepository dynamicFormRepository = mock(DynamicFormRepository.class);
    private final DynamicFormServiceImpl service = new DynamicFormServiceImpl(
            dynamicFormRepository,
            mock(FormFieldRepository.class),
            mock(WorkflowDefinitionRepository.class),
            mock(WorkflowAccessService.class)
    );

    @Test
    void validateNodeBindingsRejectsOrphanForms() {
        when(dynamicFormRepository.findByWorkflowDefinitionId("wf-1"))
                .thenReturn(List.of(DynamicForm.builder()
                        .id("form-1")
                        .workflowDefinitionId("wf-1")
                        .nodeId("Task_Review")
                        .title("Formulario revision")
                        .build()));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.validateNodeBindings("wf-1", List.of())
        );

        org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                .contains("Formulario revision")
                .contains("Task_Review");
    }

    @Test
    void validateNodeBindingsRejectsNonUserTaskBindings() {
        when(dynamicFormRepository.findByWorkflowDefinitionId("wf-1"))
                .thenReturn(List.of(DynamicForm.builder()
                        .id("form-1")
                        .workflowDefinitionId("wf-1")
                        .nodeId("Gateway_1")
                        .title("Formulario gateway")
                        .build()));

        WorkflowNode gateway = WorkflowNode.builder()
                .id("Gateway_1")
                .name("Decision")
                .type(NodeType.EXCLUSIVE_GATEWAY)
                .build();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.validateNodeBindings("wf-1", List.of(gateway))
        );

        org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                .contains("ya no es USER_TASK");
    }

    @Test
    void validateNodeBindingsAllowsExistingUserTasks() {
        when(dynamicFormRepository.findByWorkflowDefinitionId("wf-1"))
                .thenReturn(List.of(DynamicForm.builder()
                        .id("form-1")
                        .workflowDefinitionId("wf-1")
                        .nodeId("Task_Review")
                        .title("Formulario revision")
                        .build()));

        WorkflowNode userTask = WorkflowNode.builder()
                .id("Task_Review")
                .name("Revision")
                .type(NodeType.USER_TASK)
                .build();

        assertDoesNotThrow(() -> service.validateNodeBindings("wf-1", List.of(userTask)));
    }
}
