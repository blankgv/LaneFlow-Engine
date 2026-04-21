package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.admin.request.PasswordResetRequest;
import com.laneflow.engine.modules.admin.request.UserRequest;
import com.laneflow.engine.modules.admin.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserRequest request) {
        if (request.password() == null || request.password().isBlank())
            throw new IllegalArgumentException("La contraseña es requerida al crear un usuario.");
        if (userRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("Ya existe un usuario con el username: " + request.username());
        if (userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.email());

        Staff staff = findStaffOptional(request.staffId());
        Role role   = findRole(request.roleId());

        User saved = userRepository.save(User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .staffId(request.staffId())
                .roleId(request.roleId())
                .build());

        return toResponse(saved, staff, role);
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findByActiveTrueOrderByUsernameAsc()
                .stream()
                .map(u -> toResponse(u, findStaffOptional(u.getStaffId()), findRole(u.getRoleId())))
                .toList();
    }

    @Override
    public UserResponse getById(String id) {
        User user = findUser(id);
        return toResponse(user, findStaffOptional(user.getStaffId()), findRole(user.getRoleId()));
    }

    @Override
    public UserResponse update(String id, UserRequest request) {
        User user = findUser(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.email());

        Staff staff = findStaffOptional(request.staffId());
        Role role   = findRole(request.roleId());

        user.setEmail(request.email());
        user.setStaffId(request.staffId());
        user.setRoleId(request.roleId());
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user), staff, role);
    }

    @Override
    public void deactivate(String id) {
        User user = findUser(id);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String id, PasswordResetRequest request) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    private Staff findStaffOptional(String staffId) {
        if (staffId == null || staffId.isBlank()) return null;
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado: " + staffId));
    }

    private Role findRole(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));
    }

    private UserResponse toResponse(User u, Staff s, Role r) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getStaffId(),
                s != null ? s.getCode() : null,
                s != null ? s.getFirstName() + " " + s.getLastName() : null,
                u.getRoleId(),
                r.getCode(),
                r.getName(),
                u.isActive(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getLastLoginAt()
        );
    }
}
