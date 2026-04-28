package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.core.storage.StorageService;
import com.laneflow.engine.core.storage.StoredObject;
import com.laneflow.engine.modules.operation.model.Evidence;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.model.enums.EvidenceCategory;
import com.laneflow.engine.modules.operation.repository.EvidenceRepository;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.operation.response.EvidenceResponse;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.service.ProcedureAuditService;
import com.laneflow.engine.modules.workflow.model.DynamicForm;
import com.laneflow.engine.modules.workflow.model.FormField;
import com.laneflow.engine.modules.workflow.model.embedded.FileConfig;
import com.laneflow.engine.modules.workflow.model.enums.FieldType;
import com.laneflow.engine.modules.workflow.repository.DynamicFormRepository;
import com.laneflow.engine.modules.workflow.repository.FormFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceServiceImpl implements EvidenceService {

    private static final Set<FieldType> FILE_FIELD_TYPES = Set.of(
            FieldType.FILE,
            FieldType.IMAGE,
            FieldType.PHOTO,
            FieldType.AUDIO,
            FieldType.DOCUMENT,
            FieldType.VIDEO
    );

    private final EvidenceRepository evidenceRepository;
    private final ProcedureRepository procedureRepository;
    private final DynamicFormRepository dynamicFormRepository;
    private final FormFieldRepository formFieldRepository;
    private final StorageService storageService;
    private final ProcedureAuditService procedureAuditService;

    @Value("${gcp.storage.evidence-prefix:evidences}")
    private String evidencePrefix;

    @Override
    public EvidenceResponse upload(String procedureId,
                                   String taskId,
                                   String nodeId,
                                   String fieldName,
                                   String description,
                                   EvidenceCategory category,
                                   MultipartFile file,
                                   String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio.");
        }

        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + procedureId));

        EvidenceUploadContext uploadContext = resolveUploadContext(procedure, taskId, nodeId, fieldName, category);
        if (uploadContext.fieldContext() != null) {
            validateFileConfig(file, uploadContext.fieldContext().field());
        }

        String originalFileName = resolveOriginalFileName(file);
        String extension = resolveExtension(originalFileName);
        String objectName = buildObjectName(
                procedureId,
                uploadContext.nodeId(),
                uploadContext.fieldName(),
                originalFileName,
                uploadContext.fileConfig(),
                uploadContext.category()
        );
        StoredObject stored = storageService.upload(file, objectName);

        Evidence saved = evidenceRepository.save(Evidence.builder()
                .procedureId(procedureId)
                .taskId(uploadContext.taskId())
                .nodeId(uploadContext.nodeId())
                .formId(uploadContext.formId())
                .fieldId(uploadContext.fieldId())
                .fieldName(uploadContext.fieldName())
                .uploadedBy(uploadedBy)
                .fileName(objectName.substring(objectName.lastIndexOf('/') + 1))
                .originalFileName(originalFileName)
                .contentType(stored.contentType())
                .extension(extension)
                .sizeBytes(stored.sizeBytes())
                .storageProvider("GCS")
                .bucketName(stored.bucketName())
                .storagePath(stored.objectName())
                .mediaLink(stored.mediaLink())
                .description(trimToNull(description))
                .category(uploadContext.category())
                .build());

        procedureAuditService.record(
                procedure,
                ProcedureAuditAction.EVIDENCE_UPLOADED,
                "Evidencia cargada al almacenamiento.",
                uploadedBy,
                uploadContext.taskId(),
                uploadContext.nodeId(),
                null,
                procedure.getStatus(),
                procedure.getStatus(),
                buildEvidenceAuditDetails(saved)
        );

        return toResponse(saved);
    }

    @Override
    public List<EvidenceResponse> findByProcedure(String procedureId) {
        if (!procedureRepository.existsById(procedureId)) {
            throw new IllegalArgumentException("Tramite no encontrado: " + procedureId);
        }

        return evidenceRepository.findByProcedureIdOrderByCreatedAtDesc(procedureId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EvidenceResponse findById(String id) {
        return evidenceRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + id));
    }

    @Override
    public void delete(String id) {
        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + id));
        Procedure procedure = procedureRepository.findById(evidence.getProcedureId())
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + evidence.getProcedureId()));

        storageService.delete(evidence.getStoragePath());
        evidenceRepository.delete(evidence);
        procedureAuditService.record(
                procedure,
                ProcedureAuditAction.EVIDENCE_DELETED,
                "Evidencia eliminada del almacenamiento.",
                evidence.getUploadedBy(),
                evidence.getTaskId(),
                evidence.getNodeId(),
                null,
                procedure.getStatus(),
                procedure.getStatus(),
                buildEvidenceAuditDetails(evidence)
        );
    }

    private FieldContext resolveFieldContext(Procedure procedure, String nodeId, String fieldName) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("El nodeId es obligatorio para adjuntar evidencia.");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("El fieldName es obligatorio para adjuntar evidencia.");
        }

        DynamicForm form = dynamicFormRepository
                .findByWorkflowDefinitionIdAndNodeId(procedure.getWorkflowDefinitionId(), nodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Formulario no encontrado para el nodo: " + nodeId));

        FormField field = formFieldRepository.findByFormIdOrderByOrderAsc(form.getId())
                .stream()
                .filter(f -> fieldName.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campo no encontrado en el formulario del nodo: " + fieldName));

        if (!FILE_FIELD_TYPES.contains(field.getType())) {
            throw new IllegalArgumentException("El campo no permite archivos: " + fieldName);
        }

        return new FieldContext(form, field);
    }

    private EvidenceUploadContext resolveUploadContext(Procedure procedure,
                                                       String taskId,
                                                       String nodeId,
                                                       String fieldName,
                                                       EvidenceCategory requestedCategory) {
        String normalizedTaskId = trimToNull(taskId);
        String normalizedNodeId = trimToNull(nodeId);
        String normalizedFieldName = trimToNull(fieldName);

        boolean hasNode = normalizedNodeId != null;
        boolean hasField = normalizedFieldName != null;
        if (hasNode != hasField) {
            throw new IllegalArgumentException("nodeId y fieldName deben enviarse juntos para evidencia ligada a formulario.");
        }

        if (!hasNode) {
            if (normalizedTaskId != null) {
                throw new IllegalArgumentException("taskId solo se permite cuando la evidencia pertenece a una tarea/nodo.");
            }
            validateGlobalEvidenceContext(procedure);
            return new EvidenceUploadContext(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    resolveCategory(requestedCategory, null),
                    null
            );
        }

        validateEvidenceContext(procedure, normalizedTaskId, normalizedNodeId);
        FieldContext fieldContext = resolveFieldContext(procedure, normalizedNodeId, normalizedFieldName);
        return new EvidenceUploadContext(
                normalizedTaskId,
                normalizedNodeId,
                normalizedFieldName,
                fieldContext.form().getId(),
                fieldContext.field().getId(),
                fieldContext.field().getFileConfig(),
                resolveCategory(requestedCategory, fieldContext.field()),
                fieldContext
        );
    }

    private void validateEvidenceContext(Procedure procedure, String taskId, String nodeId) {
        if (procedure.getCurrentTaskId() != null && !procedure.getCurrentTaskId().isBlank()) {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("La tarea actual es obligatoria para adjuntar evidencia en un tramite activo.");
            }
            if (!procedure.getCurrentTaskId().equals(taskId)) {
                throw new IllegalArgumentException("La tarea no corresponde al tramite actual.");
            }
            if (procedure.getCurrentNodeId() == null || !procedure.getCurrentNodeId().equals(nodeId)) {
                throw new IllegalArgumentException("La evidencia solo se puede adjuntar al nodo activo del tramite.");
            }
            return;
        }

        if (procedure.getStatus() == com.laneflow.engine.modules.operation.model.enums.ProcedureStatus.OBSERVED) {
            if (procedure.getLastCompletedNodeId() == null || !procedure.getLastCompletedNodeId().equals(nodeId)) {
                throw new IllegalArgumentException("La evidencia observada solo se puede adjuntar al ultimo nodo revisado.");
            }
            return;
        }

        throw new IllegalArgumentException("No se puede adjuntar evidencia cuando el tramite no tiene una etapa habilitada.");
    }

    private void validateGlobalEvidenceContext(Procedure procedure) {
        if (procedure.getStatus() == ProcedureStatus.COMPLETED
                || procedure.getStatus() == ProcedureStatus.REJECTED
                || procedure.getStatus() == ProcedureStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede adjuntar evidencia global a un tramite cerrado.");
        }
    }

    private void validateFileConfig(MultipartFile file, FormField field) {
        FileConfig config = field.getFileConfig();
        if (config == null) {
            return;
        }

        String extension = resolveExtension(resolveOriginalFileName(file));
        if (config.getAllowedExtensions() != null && !config.getAllowedExtensions().isEmpty()) {
            boolean allowed = config.getAllowedExtensions().stream()
                    .map(this::normalizeExtension)
                    .anyMatch(ext -> ext.equals(extension));
            if (!allowed) {
                throw new IllegalArgumentException("Extension de archivo no permitida: " + extension);
            }
        }

        if (config.getMaxSizeMb() > 0) {
            long maxBytes = config.getMaxSizeMb() * 1024L * 1024L;
            if (file.getSize() > maxBytes) {
                throw new IllegalArgumentException("El archivo supera el tamano maximo permitido.");
            }
        }
    }

    private String buildObjectName(String procedureId,
                                   String nodeId,
                                   String fieldName,
                                   String originalFileName,
                                   FileConfig config,
                                   EvidenceCategory category) {
        String extension = resolveExtension(originalFileName);
        String safeName = sanitizeFileName(stripExtension(originalFileName));
        String fileName = UUID.randomUUID() + "-" + safeName + (extension.isBlank() ? "" : "." + extension);
        StringBuilder objectName = new StringBuilder(trimSlashes(evidencePrefix))
                .append("/")
                .append(procedureId)
                .append("/");

        if (nodeId != null && fieldName != null) {
            objectName.append(nodeId).append("/");
            String bucketFolder = config != null ? trimToNull(config.getBucketFolder()) : null;
            if (bucketFolder != null) {
                objectName.append(trimSlashes(bucketFolder)).append("/");
            }
            objectName.append(fieldName).append("/");
        } else {
            objectName.append("procedure/")
                    .append(category != null ? category.name().toLowerCase(Locale.ROOT) : "general")
                    .append("/");
        }
        objectName.append(fileName);
        return objectName.toString();
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "upload.bin";
        }
        return original.substring(original.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String resolveExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return "";
        return normalizeExtension(fileName.substring(idx + 1));
    }

    private String normalizeExtension(String extension) {
        if (extension == null) return "";
        return extension.trim().replace(".", "").toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? fileName : fileName.substring(0, idx);
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String safe = normalized.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return safe.isBlank() ? "file" : safe;
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) return "evidences";
        return value.replaceAll("^/+|/+$", "");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private EvidenceCategory resolveCategory(EvidenceCategory category, FormField field) {
        if (category != null) {
            return category;
        }

        if (field == null) {
            return EvidenceCategory.GENERAL;
        }

        return switch (field.getType()) {
            case IMAGE, PHOTO -> EvidenceCategory.PHOTO;
            case DOCUMENT -> EvidenceCategory.SUPPORT_DOCUMENT;
            default -> EvidenceCategory.GENERAL;
        };
    }

    private java.util.Map<String, Object> buildEvidenceAuditDetails(Evidence evidence) {
        java.util.Map<String, Object> details = new LinkedHashMap<>();
        details.put("evidenceId", evidence.getId());
        details.put("fieldName", evidence.getFieldName());
        details.put("fileName", evidence.getOriginalFileName());
        details.put("storagePath", evidence.getStoragePath());
        details.put("category", evidence.getCategory() != null ? evidence.getCategory().name() : null);
        return details;
    }

    private EvidenceResponse toResponse(Evidence e) {
        return new EvidenceResponse(
                e.getId(),
                e.getProcedureId(),
                e.getTaskId(),
                e.getNodeId(),
                e.getFormId(),
                e.getFieldId(),
                e.getFieldName(),
                e.getUploadedBy(),
                e.getFileName(),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getExtension(),
                e.getSizeBytes(),
                e.getStorageProvider(),
                e.getBucketName(),
                e.getStoragePath(),
                e.getMediaLink(),
                e.getDescription(),
                e.getCategory(),
                e.getCreatedAt()
        );
    }

    private record FieldContext(DynamicForm form, FormField field) {}

    private record EvidenceUploadContext(String taskId,
                                         String nodeId,
                                         String fieldName,
                                         String formId,
                                         String fieldId,
                                         FileConfig fileConfig,
                                         EvidenceCategory category,
                                         FieldContext fieldContext) {}
}
