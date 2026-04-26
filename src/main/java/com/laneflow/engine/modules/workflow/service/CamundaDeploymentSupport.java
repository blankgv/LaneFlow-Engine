package com.laneflow.engine.modules.workflow.service;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.List;

final class CamundaDeploymentSupport {

    private CamundaDeploymentSupport() {
    }

    static ProcessDefinition resolveProcessDefinition(
            RepositoryService repositoryService,
            Deployment deployment,
            String processKey
    ) {
        ProcessDefinition processDefinition = null;

        for (int attempt = 1; attempt <= 5 && processDefinition == null; attempt++) {
            processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .processDefinitionKey(processKey)
                    .singleResult();

            if (processDefinition == null) {
                List<ProcessDefinition> byDeployment = repositoryService.createProcessDefinitionQuery()
                        .deploymentId(deployment.getId())
                        .list();

                if (byDeployment.size() == 1) {
                    processDefinition = byDeployment.get(0);
                }
            }

            if (processDefinition == null && attempt < 5) {
                try {
                    Thread.sleep(150L * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("La consulta a Camunda fue interrumpida.", interruptedException);
                }
            }
        }

        if (processDefinition == null) {
            List<ProcessDefinition> byDeployment = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .list();
            String availableKeys = byDeployment.stream()
                    .map(ProcessDefinition::getKey)
                    .distinct()
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("ninguna");

            throw new IllegalStateException(
                    "Camunda no registro una processDefinition utilizable para la key '" + processKey + "'. " +
                    "Definitions detectadas en el deployment: " + availableKeys
            );
        }

        return processDefinition;
    }
}
