package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.request.DepartmentRequest;
import com.laneflow.engine.modules.admin.response.DepartmentResponse;
import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    @Override
    public DepartmentResponse create(DepartmentRequest request) {
        if (repository.existsByCode(request.code()))
            throw new IllegalArgumentException("Ya existe un departamento con el código: " + request.code());

        Department saved = repository.save(Department.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .parentId(request.parentId())
                .build());

        return toResponse(saved);
    }

    @Override
    public List<DepartmentResponse> getAll() {
        return repository.findByActiveTrueOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DepartmentResponse getById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Departamento no encontrado: " + id));
    }

    @Override
    public DepartmentResponse update(String id, DepartmentRequest request) {
        Department dept = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Departamento no encontrado: " + id));

        boolean codeChanged = !dept.getCode().equalsIgnoreCase(request.code());
        if (codeChanged && repository.existsByCode(request.code()))
            throw new IllegalArgumentException("Ya existe un departamento con el código: " + request.code());

        dept.setCode(request.code().toUpperCase());
        dept.setName(request.name());
        dept.setDescription(request.description());
        dept.setParentId(request.parentId());
        dept.setUpdatedAt(LocalDateTime.now());

        return toResponse(repository.save(dept));
    }

    @Override
    public void deactivate(String id) {
        Department dept = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Departamento no encontrado: " + id));

        dept.setActive(false);
        dept.setUpdatedAt(LocalDateTime.now());
        repository.save(dept);
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getDescription(),
                d.getParentId(),
                d.isActive(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
