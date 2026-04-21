package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.request.RoleRequest;
import com.laneflow.engine.modules.admin.response.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByCode(request.code()))
            throw new IllegalArgumentException("Ya existe un rol con el código: " + request.code());

        validatePermissions(request.permissions());

        Role saved = roleRepository.save(Role.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .permissions(request.permissions())
                .build());

        return toResponse(saved);
    }

    @Override
    public List<RoleResponse> getAll() {
        return roleRepository.findByActiveTrueOrderByCodeAsc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public RoleResponse getById(String id) {
        return toResponse(findRole(id));
    }

    @Override
    public RoleResponse update(String id, RoleRequest request) {
        Role role = findRole(id);

        if (!role.getCode().equalsIgnoreCase(request.code()) && roleRepository.existsByCode(request.code()))
            throw new IllegalArgumentException("Ya existe un rol con el código: " + request.code());

        validatePermissions(request.permissions());

        role.setCode(request.code().toUpperCase());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(request.permissions());
        role.setUpdatedAt(LocalDateTime.now());

        return toResponse(roleRepository.save(role));
    }

    @Override
    public void deactivate(String id) {
        Role role = findRole(id);
        role.setActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    private void validatePermissions(List<String> permissions) {
        List<String> invalid = permissions.stream()
                .filter(p -> !Permission.ALL.contains(p))
                .toList();
        if (!invalid.isEmpty())
            throw new IllegalArgumentException("Permisos no válidos: " + invalid);
    }

    private Role findRole(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
    }

    private RoleResponse toResponse(Role r) {
        return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                r.getPermissions(), r.isActive(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
