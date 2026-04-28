package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
import com.laneflow.engine.modules.operation.request.ApplicantRequest;
import com.laneflow.engine.modules.operation.response.ApplicantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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

        String initialPassword = createApplicantUser(saved);
        return toResponseWithCredentials(saved, initialPassword);
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
        Optional<User> user = userRepository.findByApplicantId(a.getId());
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
                a.getUpdatedAt(),
                user.map(User::getId).orElse(null),
                user.map(User::getUsername).orElse(null),
                null
        );
    }

    private ApplicantResponse toResponseWithCredentials(Applicant a, String initialPassword) {
        Optional<User> user = userRepository.findByApplicantId(a.getId());
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
                a.getUpdatedAt(),
                user.map(User::getId).orElse(null),
                user.map(User::getUsername).orElse(null),
                initialPassword
        );
    }

    /** Creates applicant user. Returns plaintext initial password, or null if user already existed. */
    private String createApplicantUser(Applicant applicant) {
        String username = applicant.getDocumentNumber().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            return null;
        }
        String rawPassword = applicant.getDocumentNumber();
        String roleId = roleRepository.findByCode("APPLICANT")
                .map(Role::getId)
                .orElse(null);

        User.UserBuilder builder = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .applicantId(applicant.getId())
                .roleId(roleId);

        if (applicant.getEmail() != null) {
            builder.email(applicant.getEmail());
        }

        userRepository.save(builder.build());

        return rawPassword;
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
