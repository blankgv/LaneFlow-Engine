package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.admin.request.StaffRequest;
import com.laneflow.engine.modules.admin.response.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public StaffResponse create(StaffRequest request) {
        if (staffRepository.existsByCode(request.code()))
            throw new IllegalArgumentException("Ya existe personal con el código: " + request.code());
        if (staffRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Ya existe personal con el email: " + request.email());

        Department dept = findDepartment(request.departmentId());

        Staff saved = staffRepository.save(Staff.builder()
                .code(request.code().toUpperCase())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .departmentId(request.departmentId())
                .build());

        return toResponse(saved, dept);
    }

    @Override
    public List<StaffResponse> getAll() {
        return staffRepository.findByActiveTrueOrderByCodeAsc()
                .stream()
                .map(s -> toResponse(s, findDepartment(s.getDepartmentId())))
                .toList();
    }

    @Override
    public StaffResponse getById(String id) {
        Staff staff = findStaff(id);
        return toResponse(staff, findDepartment(staff.getDepartmentId()));
    }

    @Override
    public StaffResponse update(String id, StaffRequest request) {
        Staff staff = findStaff(id);

        if (staffRepository.existsByCodeAndIdNot(request.code(), id))
            throw new IllegalArgumentException("Ya existe personal con el código: " + request.code());
        if (staffRepository.existsByEmailAndIdNot(request.email(), id))
            throw new IllegalArgumentException("Ya existe personal con el email: " + request.email());

        Department dept = findDepartment(request.departmentId());

        staff.setCode(request.code().toUpperCase());
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setEmail(request.email());
        staff.setPhone(request.phone());
        staff.setDepartmentId(request.departmentId());
        staff.setUpdatedAt(LocalDateTime.now());

        return toResponse(staffRepository.save(staff), dept);
    }

    @Override
    public void deactivate(String id) {
        Staff staff = findStaff(id);
        staff.setActive(false);
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
    }

    private Staff findStaff(String id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado: " + id));
    }

    private Department findDepartment(String departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Departamento no encontrado: " + departmentId));
    }

    private StaffResponse toResponse(Staff s, Department d) {
        return new StaffResponse(
                s.getId(),
                s.getCode(),
                s.getFirstName(),
                s.getLastName(),
                s.getEmail(),
                s.getPhone(),
                s.getDepartmentId(),
                d.getCode(),
                d.getName(),
                s.isActive(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
