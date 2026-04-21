package com.laneflow.engine.modules.admin.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.request.PasswordResetRequest;
import com.laneflow.engine.modules.admin.request.UserRequest;
import com.laneflow.engine.modules.admin.response.UserResponse;
import com.laneflow.engine.modules.admin.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.USER_WRITE + "')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.USER_READ + "')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.USER_READ + "')")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.USER_WRITE + "')")
    public ResponseEntity<UserResponse> update(@PathVariable String id,
                                                @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.USER_WRITE + "')")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('" + Permission.USER_WRITE + "')")
    public ResponseEntity<Void> resetPassword(@PathVariable String id,
                                               @Valid @RequestBody PasswordResetRequest request) {
        service.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
