# Guia de implementacion - Modulo 3: Design

## 1. Objetivo del modulo

El modulo `Design` define la configuracion del flujo de trabajo. En el backend actual cubre:

- workflows
- versiones de workflow
- formularios dinamicos
- campos de formulario
- politicas de negocio

Referencia funcional base: [SYSTEM_SPECS.md](/D:/Docs/Desktop/code/laneflow-engine/SYSTEM_SPECS.md)

## 2. Seguridad

Todo el modulo requiere JWT valido.

Permisos:

- lectura: `WORKFLOW_READ`
- escritura: `WORKFLOW_WRITE`

Header requerido:

```http
Authorization: Bearer <JWT>
```

## 3. Mapa del modulo

Base endpoints:

1. `/api/v1/workflows`
2. `/api/v1/workflows/{workflowId}/versions`
3. `/api/v1/forms`
4. `/api/v1/policies`

Controllers fuente:

- [WorkflowController.java](/D:/Docs/Desktop/code/laneflow-engine/src/main/java/com/laneflow/engine/modules/workflow/controller/WorkflowController.java)
- [WorkflowVersionController.java](/D:/Docs/Desktop/code/laneflow-engine/src/main/java/com/laneflow/engine/modules/workflow/controller/WorkflowVersionController.java)
- [DynamicFormController.java](/D:/Docs/Desktop/code/laneflow-engine/src/main/java/com/laneflow/engine/modules/workflow/controller/DynamicFormController.java)
- [BusinessPolicyController.java](/D:/Docs/Desktop/code/laneflow-engine/src/main/java/com/laneflow/engine/modules/workflow/controller/BusinessPolicyController.java)

## 4. Conceptos clave del dominio

### Workflow

Es la definicion general del proceso:

- codigo
- nombre
- swimlanes
- nodos
- transiciones
- estado

### Workflow version

Es un snapshot versionado del proceso, hoy orientado sobre todo a guardar y publicar BPMN XML.

### Dynamic form

Es el formulario asociado a un nodo del workflow.

### Form field

Es un campo dentro de un formulario, con tipo, orden, validaciones y configuracion opcional de archivo.

### Business policy

Es una regla con condiciones y acciones que se asocia a un workflow y opcionalmente a un nodo.

## 5. Catalogos y enums que el front debe conocer

### 5.1 Tipos de nodo

- `START_EVENT`
- `END_EVENT`
- `USER_TASK`
- `SERVICE_TASK`
- `EXCLUSIVE_GATEWAY`
- `PARALLEL_GATEWAY`
- `INCLUSIVE_GATEWAY`

### 5.2 Estados de workflow

- `DRAFT`
- `PUBLISHED`
- `DEPRECATED`

### 5.3 Estados de version

- `DRAFT`
- `PUBLISHED`
- `DEPRECATED`

### 5.4 Tipos de campo

- `TEXT`
- `TEXTAREA`
- `NUMBER`
- `DATE`
- `DATETIME`
- `SELECT`
- `MULTISELECT`
- `CHECKBOX`
- `RADIO`
- `FILE`
- `IMAGE`
- `AUDIO`
- `DOCUMENT`
- `VIDEO`
- `GENERIC`

### 5.5 Operadores logicos

- `AND`
- `OR`

### 5.6 Operadores de condicion de politica

- `EQUALS`
- `NOT_EQUALS`
- `GREATER_THAN`
- `GREATER_THAN_OR_EQUAL`
- `LESS_THAN`
- `LESS_THAN_OR_EQUAL`
- `CONTAINS`
- `NOT_CONTAINS`
- `IS_NULL`
- `IS_NOT_NULL`

### 5.7 Tipos de accion de politica

- `ROUTE_TO_NODE`
- `SEND_NOTIFICATION`
- `REJECT`
- `ASSIGN_TO_DEPARTMENT`
- `SET_VARIABLE`

## 6. Reglas generales del modulo

- no hay paginacion ni filtros avanzados
- `Workflow` se puede editar o eliminar solo cuando esta en `DRAFT`
- un workflow debe tener al menos un `START_EVENT` y un `END_EVENT`
- las transiciones deben apuntar a nodos existentes
- publicar workflow despliega BPMN en Camunda y genera snapshot en `workflow_versions`
- las versiones publicadas previas pasan a `DEPRECATED` al publicar una nueva version
- eliminar formulario borra tambien sus campos
- `toggle` de politica solo cambia `active`

Formato comun de error:

```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensaje principal",
  "path": "/api/v1/...",
  "validationErrors": {
    "field": "detalle"
  }
}
```

## 7. Dependencias recomendadas de implementacion

Orden sugerido para el front:

1. listado y editor de workflows
2. publicacion y validacion
3. versiones
4. formularios por nodo
5. politicas

Dependencias practicas:

- formularios dependen de un workflow y de un nodo
- politicas dependen de un workflow y normalmente de un nodo
- swimlanes pueden referenciar departamentos del modulo `Admin`
- nodos `USER_TASK` pueden asociarse a `departmentId`

## 8. Endpoints de Workflows

Base:

```text
/api/v1/workflows
```

### 8.1 Listar workflows

```http
GET /api/v1/workflows
Authorization: Bearer <JWT>
```

Response `200 OK`:

```json
[
  {
    "id": "wf1",
    "code": "LICENCIA",
    "name": "Licencia de funcionamiento",
    "description": "Workflow base",
    "status": "DRAFT",
    "currentVersion": 1,
    "createdAt": "2026-04-25T10:00:00",
    "updatedAt": null
  }
]
```

### 8.2 Crear workflow

```http
POST /api/v1/workflows
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "code": "LICENCIA",
  "name": "Licencia de funcionamiento",
  "description": "Flujo de aprobacion",
  "swimlanes": [
    {
      "id": "lane-intake",
      "name": "Recepcion",
      "departmentId": "dept-1",
      "departmentCode": "ATC"
    }
  ],
  "nodes": [
    {
      "id": "start-1",
      "name": "Inicio",
      "type": "START_EVENT",
      "swimlaneId": "lane-intake",
      "departmentId": null,
      "formKey": null,
      "requiredAction": null
    },
    {
      "id": "task-1",
      "name": "Registrar solicitud",
      "type": "USER_TASK",
      "swimlaneId": "lane-intake",
      "departmentId": "dept-1",
      "formKey": "form-registro",
      "requiredAction": "REGISTER"
    },
    {
      "id": "end-1",
      "name": "Fin",
      "type": "END_EVENT",
      "swimlaneId": null,
      "departmentId": null,
      "formKey": null,
      "requiredAction": null
    }
  ],
  "transitions": [
    {
      "id": "flow-1",
      "sourceNodeId": "start-1",
      "targetNodeId": "task-1",
      "condition": null,
      "label": "Inicio"
    },
    {
      "id": "flow-2",
      "sourceNodeId": "task-1",
      "targetNodeId": "end-1",
      "condition": null,
      "label": "Completar"
    }
  ]
}
```

Validaciones declarativas:

- `code`: requerido
- `name`: requerido
- `nodes[].name`: requerido
- `nodes[].type`: requerido
- `transitions[].sourceNodeId`: requerido
- `transitions[].targetNodeId`: requerido
- `swimlanes[].name`: requerido

Validaciones de negocio:

- codigo unico
- al menos un nodo
- al menos un `START_EVENT`
- al menos un `END_EVENT`
- source y target de cada transicion deben existir

Response `201 Created`:

```json
{
  "id": "wf1",
  "code": "LICENCIA",
  "name": "Licencia de funcionamiento",
  "description": "Flujo de aprobacion",
  "status": "DRAFT",
  "currentVersion": 1,
  "camundaProcessKey": "process_licencia",
  "swimlanes": [
    {
      "id": "lane-intake",
      "name": "Recepcion",
      "departmentId": "dept-1",
      "departmentCode": "ATC"
    }
  ],
  "nodes": [
    {
      "id": "start-1",
      "name": "Inicio",
      "type": "START_EVENT",
      "swimlaneId": "lane-intake",
      "departmentId": null,
      "formKey": null,
      "requiredAction": null
    }
  ],
  "transitions": [],
  "createdAt": "2026-04-25T10:00:00",
  "updatedAt": null,
  "publishedAt": null
}
```

### 8.3 Obtener workflow por ID

```http
GET /api/v1/workflows/{id}
Authorization: Bearer <JWT>
```

Response `200 OK` usa el mismo esquema de `WorkflowResponse`.

### 8.4 Actualizar workflow

```http
PUT /api/v1/workflows/{id}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Licencia comercial",
  "description": "Flujo actualizado",
  "swimlanes": [],
  "nodes": [],
  "transitions": []
}
```

Reglas:

- solo permite editar workflows `DRAFT`
- cualquier bloque enviado reemplaza la coleccion completa correspondiente
- si se actualizan nodos y transiciones, la estructura vuelve a validarse

### 8.5 Eliminar workflow

```http
DELETE /api/v1/workflows/{id}
Authorization: Bearer <JWT>
```

Response `204 No Content`

Regla:

- solo se elimina si esta en `DRAFT`

### 8.6 Publicar workflow

```http
POST /api/v1/workflows/{id}/publish
Authorization: Bearer <JWT>
```

Comportamiento:

- valida estructura
- genera BPMN XML desde los nodos y transiciones
- despliega en Camunda
- guarda snapshot en `workflow_versions`
- cambia estado del workflow a `PUBLISHED`

Response `200 OK` con `WorkflowResponse`.

### 8.7 Validar workflow

```http
GET /api/v1/workflows/{id}/validate
Authorization: Bearer <JWT>
```

Comportamiento:

- no persiste cambios
- valida estructura actual
- devuelve el workflow si es valido

## 9. Endpoints de Workflow Versions

Base:

```text
/api/v1/workflows/{workflowId}/versions
```

### 9.1 Listar versiones

```http
GET /api/v1/workflows/{workflowId}/versions
Authorization: Bearer <JWT>
```

Response `200 OK`:

```json
[
  {
    "id": "ver-2",
    "workflowDefinitionId": "wf1",
    "versionNumber": 2,
    "bpmnXml": "<definitions>...</definitions>",
    "status": "DRAFT",
    "camundaDeploymentId": null,
    "createdAt": "2026-04-25T10:00:00",
    "publishedAt": null
  }
]
```

### 9.2 Obtener version especifica

```http
GET /api/v1/workflows/{workflowId}/versions/{versionNumber}
Authorization: Bearer <JWT>
```

### 9.3 Crear version draft

```http
POST /api/v1/workflows/{workflowId}/versions
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions>...</definitions>"
}
```

Request DTO:

```json
{
  "bpmnXml": "string | null"
}
```

Comportamiento:

- el numero de version se calcula automaticamente
- estado inicial: `DRAFT`
- guarda `createdBy`

Response `201 Created`:

```json
{
  "id": "ver-3",
  "workflowDefinitionId": "wf1",
  "versionNumber": 3,
  "bpmnXml": "<definitions>...</definitions>",
  "status": "DRAFT",
  "camundaDeploymentId": null,
  "createdAt": "2026-04-25T10:00:00",
  "publishedAt": null
}
```

### 9.4 Publicar version

```http
POST /api/v1/workflows/{workflowId}/versions/{versionNumber}/publish
Authorization: Bearer <JWT>
```

Comportamiento:

- si habia una version `PUBLISHED`, pasa a `DEPRECATED`
- si `bpmnXml` tiene contenido, despliega en Camunda
- la version queda `PUBLISHED`

Observacion:

- hoy puede quedar una version `PUBLISHED` aun si `bpmnXml` viene vacio, porque el servicio solo omite el despliegue pero igual cambia el estado. Conviene endurecer esto si el front va a exponer versionado BPMN real.

## 10. Endpoints de Forms

Base:

```text
/api/v1/forms
```

### 10.1 Listar formularios por workflow

```http
GET /api/v1/forms?workflowId=wf1
Authorization: Bearer <JWT>
```

Response `200 OK`:

```json
[
  {
    "id": "form-1",
    "workflowDefinitionId": "wf1",
    "nodeId": "task-1",
    "nodeName": "Registrar solicitud",
    "title": "Formulario de registro",
    "fields": [],
    "createdAt": "2026-04-25T10:00:00",
    "updatedAt": null
  }
]
```

### 10.2 Crear formulario

```http
POST /api/v1/forms
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "workflowDefinitionId": "wf1",
  "nodeId": "task-1",
  "nodeName": "Registrar solicitud",
  "title": "Formulario de registro"
}
```

Validaciones:

- `workflowDefinitionId`: requerido
- `nodeId`: requerido
- `nodeName`: requerido
- `title`: requerido

Response `201 Created` usa `DynamicFormResponse`.

Observacion importante:

- hoy el backend no valida si `workflowDefinitionId` existe ni si `nodeId` pertenece al workflow al crear formularios. El front deberia controlar esto por UX.

### 10.3 Obtener formulario por ID

```http
GET /api/v1/forms/{id}
Authorization: Bearer <JWT>
```

### 10.4 Actualizar formulario

```http
PUT /api/v1/forms/{id}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "nodeName": "Registrar tramite",
  "title": "Formulario principal"
}
```

### 10.5 Eliminar formulario

```http
DELETE /api/v1/forms/{id}
Authorization: Bearer <JWT>
```

Response `204 No Content`

Comportamiento:

- elimina tambien todos los campos del formulario

## 11. Endpoints de Fields dentro del formulario

### 11.1 Agregar campo

```http
POST /api/v1/forms/{formId}/fields
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "applicantName",
  "label": "Nombre del solicitante",
  "type": "TEXT",
  "required": true,
  "order": 1,
  "options": null,
  "validations": [
    {
      "type": "MIN_LENGTH",
      "value": "3",
      "message": "Debe tener al menos 3 caracteres"
    }
  ],
  "fileConfig": null
}
```

Validaciones declarativas:

- `name`: requerido
- `label`: requerido
- `type`: requerido

Response `201 Created`:

```json
{
  "id": "field-1",
  "formId": "form-1",
  "name": "applicantName",
  "label": "Nombre del solicitante",
  "type": "TEXT",
  "required": true,
  "order": 1,
  "options": null,
  "validations": [
    {
      "type": "MIN_LENGTH",
      "value": "3",
      "message": "Debe tener al menos 3 caracteres"
    }
  ],
  "fileConfig": null,
  "createdAt": "2026-04-25T10:00:00",
  "updatedAt": null
}
```

### 11.2 Actualizar campo

```http
PUT /api/v1/forms/{formId}/fields/{fieldId}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "label": "Nombre completo del solicitante",
  "required": true,
  "order": 2
}
```

Reglas:

- valida que el campo pertenezca al formulario
- actualiza solo propiedades enviadas

### 11.3 Eliminar campo

```http
DELETE /api/v1/forms/{formId}/fields/{fieldId}
Authorization: Bearer <JWT>
```

Response `204 No Content`

### 11.4 Reordenar campos

```http
PUT /api/v1/forms/{formId}/fields/reorder
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "fields": [
    { "fieldId": "field-1", "order": 1 },
    { "fieldId": "field-2", "order": 2 }
  ]
}
```

Response `200 OK` devuelve la lista de campos ordenada.

Observacion:

- si en la lista se manda un `fieldId` que no pertenece al formulario, hoy simplemente se ignora.

## 12. Endpoints de Policies

Base:

```text
/api/v1/policies
```

### 12.1 Listar politicas por workflow

```http
GET /api/v1/policies?workflowId=wf1
Authorization: Bearer <JWT>
```

Response `200 OK`:

```json
[
  {
    "id": "pol-1",
    "name": "Ruta prioritaria",
    "description": "Enrutamiento segun monto",
    "workflowDefinitionId": "wf1",
    "nodeId": "task-1",
    "active": true,
    "priority": 1,
    "conditions": [
      {
        "field": "amount",
        "operator": "GREATER_THAN",
        "value": "10000",
        "logicalOperator": "AND"
      }
    ],
    "actions": [
      {
        "type": "ROUTE_TO_NODE",
        "targetNodeId": "task-approver",
        "targetNodeName": "Aprobacion gerencial",
        "targetDepartmentId": null,
        "variableName": null,
        "variableValue": null,
        "notificationMessage": null
      }
    ],
    "createdAt": "2026-04-25T10:00:00",
    "updatedAt": null
  }
]
```

### 12.2 Crear politica

```http
POST /api/v1/policies
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Ruta prioritaria",
  "description": "Enrutamiento segun monto",
  "workflowDefinitionId": "wf1",
  "nodeId": "task-1",
  "priority": 1,
  "conditions": [
    {
      "field": "amount",
      "operator": "GREATER_THAN",
      "value": "10000",
      "logicalOperator": "AND"
    }
  ],
  "actions": [
    {
      "type": "ROUTE_TO_NODE",
      "targetNodeId": "task-approver",
      "targetNodeName": "Aprobacion gerencial",
      "targetDepartmentId": null,
      "variableName": null,
      "variableValue": null,
      "notificationMessage": null
    }
  ]
}
```

Validaciones:

- `name`: requerido
- `workflowDefinitionId`: requerido
- `nodeId`: requerido
- `conditions`: no vacio
- `actions`: no vacio
- `conditions[].field`: requerido
- `conditions[].operator`: requerido
- `actions[].type`: requerido

Reglas de negocio:

- el workflow debe existir
- si `logicalOperator` no llega, se asigna `AND`

Observacion:

- hoy no se valida que `nodeId` exista dentro del workflow ni que la accion apunte a nodos o departamentos reales.

### 12.3 Obtener politica por ID

```http
GET /api/v1/policies/{id}
Authorization: Bearer <JWT>
```

### 12.4 Actualizar politica

```http
PUT /api/v1/policies/{id}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "priority": 2,
  "conditions": [
    {
      "field": "amount",
      "operator": "GREATER_THAN_OR_EQUAL",
      "value": "5000",
      "logicalOperator": "AND"
    }
  ]
}
```

Reglas:

- actualiza solo lo enviado
- `workflowDefinitionId` viene en DTO pero el servicio actual no lo usa

### 12.5 Toggle active

```http
PATCH /api/v1/policies/{id}/toggle
Authorization: Bearer <JWT>
```

Response `200 OK` con `BusinessPolicyResponse`.

### 12.6 Eliminar politica

```http
DELETE /api/v1/policies/{id}
Authorization: Bearer <JWT>
```

Response `204 No Content`

## 13. Qué necesita el frontend para implementar Design

### 13.1 Pantallas o modulos minimos

1. listado de workflows
2. editor visual de workflow
3. detalle de workflow
4. versionado de workflow
5. builder de formularios
6. builder de politicas

### 13.2 Estado de editor que el front deberia manejar

Minimo para el editor:

```ts
type WorkflowEditorState = {
  workflowId: string | null;
  code: string;
  name: string;
  description: string | null;
  status: "DRAFT" | "PUBLISHED" | "DEPRECATED";
  swimlanes: Swimlane[];
  nodes: WorkflowNode[];
  transitions: WorkflowTransition[];
};
```

### 13.3 Servicios HTTP minimos

```ts
getWorkflows()
getWorkflowById(id)
createWorkflow(payload)
updateWorkflow(id, payload)
deleteWorkflow(id)
publishWorkflow(id)
validateWorkflow(id)

getWorkflowVersions(workflowId)
getWorkflowVersion(workflowId, versionNumber)
createWorkflowVersion(workflowId, payload)
publishWorkflowVersion(workflowId, versionNumber)

getForms(workflowId)
getFormById(id)
createForm(payload)
updateForm(id, payload)
deleteForm(id)
addField(formId, payload)
updateField(formId, fieldId, payload)
deleteField(formId, fieldId)
reorderFields(formId, payload)

getPolicies(workflowId)
getPolicyById(id)
createPolicy(payload)
updatePolicy(id, payload)
togglePolicy(id)
deletePolicy(id)
```

### 13.4 Catalogos auxiliares recomendados

El front deberia tener:

- catalogo de departamentos desde `Admin`
- catalogo local de `NodeType`
- catalogo local de `FieldType`
- catalogo local de `PolicyConditionOperator`
- catalogo local de `PolicyActionType`

### 13.5 Componentes UX recomendados

Para `Workflow`:

- canvas o diagrama visual
- inspector lateral de nodo
- editor de transiciones
- panel de validacion

Para `Forms`:

- listado de formularios por nodo
- constructor de campos
- drag and drop o reorder simple
- editor de validaciones

Para `Policies`:

- tabla de politicas por workflow
- builder de condiciones
- builder de acciones
- switch de activo/inactivo

## 14. Tipos sugeridos para frontend

```ts
export type Swimlane = {
  id: string;
  name: string;
  departmentId: string | null;
  departmentCode: string | null;
};

export type WorkflowNode = {
  id: string;
  name: string;
  type: NodeType;
  swimlaneId: string | null;
  departmentId: string | null;
  formKey: string | null;
  requiredAction: string | null;
};

export type WorkflowTransition = {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  condition: string | null;
  label: string | null;
};

export type DynamicForm = {
  id: string;
  workflowDefinitionId: string;
  nodeId: string;
  nodeName: string;
  title: string;
  fields: FormField[];
  createdAt: string;
  updatedAt: string | null;
};

export type FormField = {
  id: string;
  formId: string;
  name: string;
  label: string;
  type: FieldType;
  required: boolean;
  order: number;
  options: string[] | null;
  validations: FieldValidation[] | null;
  fileConfig: FileConfig | null;
  createdAt: string;
  updatedAt: string | null;
};

export type BusinessPolicy = {
  id: string;
  name: string;
  description: string | null;
  workflowDefinitionId: string;
  nodeId: string;
  active: boolean;
  priority: number;
  conditions: PolicyCondition[];
  actions: PolicyAction[];
  createdAt: string;
  updatedAt: string | null;
};
```

## 15. Casos de error que el front debe contemplar

- codigo de workflow duplicado
- workflow sin nodos
- workflow sin `START_EVENT`
- workflow sin `END_EVENT`
- transicion con source o target invalido
- intento de editar workflow publicado
- intento de eliminar workflow publicado
- workflow inexistente
- version inexistente
- version ya publicada
- formulario inexistente
- campo inexistente
- campo que no pertenece al formulario
- politica inexistente
- workflow inexistente al crear politica
- error de despliegue en Camunda al publicar

## 16. Observaciones importantes del contrato actual

Conviene alinear estos puntos antes de escalar el modulo:

1. `Workflow.currentVersion` no se incrementa cuando se crean o publican nuevas versiones
2. `createDraft` de version no valida si `bpmnXml` existe, es valido o no vacio
3. publicar una version con `bpmnXml` vacio hoy puede dejarla `PUBLISHED` sin deployment
4. formularios no validan existencia de workflow ni pertenencia de nodo
5. politicas no validan existencia de `nodeId`, `targetNodeId` o `targetDepartmentId`
6. `UpdatePolicyRequest` incluye `workflowDefinitionId`, pero el servicio actual no lo usa
7. el endpoint de workflow principal publica desde la estructura JSON de nodos/transiciones, mientras que el endpoint de versiones publica desde `bpmnXml`; el front debe decidir con claridad si manejar ambos modos o solo uno

## 17. Fases sugeridas de implementacion

### Fase 1

- CRUD de workflows
- validacion de estructura
- publicacion basica

### Fase 2

- builder de formularios por nodo
- CRUD de campos
- reorder de campos

### Fase 3

- builder de politicas
- versionado BPMN
- historial y detalle de versiones

---

Si quieres, el siguiente paso puede ser:

1. guía del módulo `Operation`
2. guía del módulo `Tracking`
3. propuesta de contratos mejorados para `Design`
