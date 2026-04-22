package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
import com.laneflow.engine.modules.operation.request.ApplicantRequest;
import com.laneflow.engine.modules.operation.response.ApplicantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository repository;

    @Override
    public ApplicantResponse create(ApplicantRequest request) {
        validateApplicantData(request);

        String documentNumber = normalizeDocumentNumber(request.documentNumber());
        if (repository.existsByDocumentNumber(documentNumber)) {
            throw new IllegalArgumentException("Ya existe un solicitante con el documento: " + documentNumber);
        }

        Applicant saved = repository.save(Applicant.builder()
                .type(request.type())
                .documentType(request.documentType())
                .documentNumber(documentNumber)
                .firstName(trimToNull(request.firstName()))
                .lastName(trimToNull(request.lastName()))
                .businessName(trimToNull(request.businessName()))
                .legalRepresentative(trimToNull(request.legalRepresentative()))
                .email(trimToNull(request.email()))
                .phone(trimToNull(request.phone()))
                .address(trimToNull(request.address()))
                .build());

        return toResponse(saved);
    }

    @Override
    public List<ApplicantResponse> getAll() {
        return repository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ApplicantResponse getById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Solicitante no encontrado: " + id));
    }

    @Override
    public ApplicantResponse update(String id, ApplicantRequest request) {
        validateApplicantData(request);

        Applicant applicant = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitante no encontrado: " + id));

        String documentNumber = normalizeDocumentNumber(request.documentNumber());
        boolean documentChanged = !documentNumber.equalsIgnoreCase(applicant.getDocumentNumber());
        if (documentChanged && repository.existsByDocumentNumber(documentNumber)) {
            throw new IllegalArgumentException("Ya existe un solicitante con el documento: " + documentNumber);
        }

        applicant.setType(request.type());
        applicant.setDocumentType(request.documentType());
        applicant.setDocumentNumber(documentNumber);
        applicant.setFirstName(trimToNull(request.firstName()));
        applicant.setLastName(trimToNull(request.lastName()));
        applicant.setBusinessName(trimToNull(request.businessName()));
        applicant.setLegalRepresentative(trimToNull(request.legalRepresentative()));
        applicant.setEmail(trimToNull(request.email()));
        applicant.setPhone(trimToNull(request.phone()));
        applicant.setAddress(trimToNull(request.address()));
        applicant.setUpdatedAt(LocalDateTime.now());

        return toResponse(repository.save(applicant));
    }

    @Override
    public void deactivate(String id) {
        Applicant applicant = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitante no encontrado: " + id));

        applicant.setActive(false);
        applicant.setUpdatedAt(LocalDateTime.now());
        repository.save(applicant);
    }

    private void validateApplicantData(ApplicantRequest request) {
        if (request.type() == ApplicantType.NATURAL_PERSON) {
            if (isBlank(request.firstName())) {
                throw new IllegalArgumentException("El nombre es obligatorio para una persona natural.");
            }
            if (isBlank(request.lastName())) {
                throw new IllegalArgumentException("El apellido es obligatorio para una persona natural.");
            }
        }

        if (request.type() == ApplicantType.LEGAL_ENTITY && isBlank(request.businessName())) {
            throw new IllegalArgumentException("La razon social es obligatoria para una persona juridica.");
        }
    }

    private ApplicantResponse toResponse(Applicant a) {
        return new ApplicantResponse(
                a.getId(),
                a.getType(),
                a.getDocumentType(),
                a.getDocumentNumber(),
                a.getFirstName(),
                a.getLastName(),
                a.getBusinessName(),
                a.getLegalRepresentative(),
                a.getEmail(),
                a.getPhone(),
                a.getAddress(),
                a.isActive(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private String normalizeDocumentNumber(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
