package com.laneflow.engine.core.seeder;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.auth.model.User;
import com.laneflow.engine.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedDepartments();
        seedStaff();
        seedRoles();
        seedUsers();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            log.info("Departamentos ya existen, omitiendo seed.");
            return;
        }
        Department ti = departmentRepository.save(dept("TI", "Tecnología de Información", null));
        departmentRepository.saveAll(List.of(
                dept("MGMT", "Gerencia General",    null),
                dept("RRHH", "Recursos Humanos",    null),
                dept("DEV",  "Desarrollo",          ti.getId()),
                dept("INF",  "Infraestructura",     ti.getId()),
                dept("FIN",  "Finanzas",            null),
                dept("OPS",  "Operaciones",         null),
                dept("LEG",  "Legal",               null),
                dept("ATC",  "Atención al Cliente", null)
        ));
        log.info("Seed: 9 departamentos insertados.");
    }

    private void seedStaff() {
        if (staffRepository.count() > 0) {
            log.info("Personal ya existe, omitiendo seed.");
            return;
        }
        Map<String, String> deptIds = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getCode, Department::getId));

        staffRepository.saveAll(List.of(
                staff("EMP-001", "Carlos",  "Mendoza",  "c.mendoza@laneflow.com",  deptIds.get("MGMT")),
                staff("EMP-002", "María",   "López",    "m.lopez@laneflow.com",    deptIds.get("RRHH")),
                staff("EMP-003", "Andrés",  "Torres",   "a.torres@laneflow.com",   deptIds.get("DEV")),
                staff("EMP-004", "Lucía",   "Ramírez",  "l.ramirez@laneflow.com",  deptIds.get("FIN")),
                staff("EMP-005", "Diego",   "Herrera",  "d.herrera@laneflow.com",  deptIds.get("ATC")),
                staff("EMP-006", "Sofía",   "Vargas",   "s.vargas@laneflow.com",   deptIds.get("OPS")),
                staff("EMP-007", "Roberto", "Castro",   "r.castro@laneflow.com",   deptIds.get("LEG"))
        ));
        log.info("Seed: 7 miembros de personal insertados.");
    }

    private void seedRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles ya existen, omitiendo seed.");
            return;
        }
        roleRepository.saveAll(List.of(
                role("ADMINISTRADOR", "Administrador",    "Acceso total al sistema", Permission.ALL),
                role("FUNCIONARIO",   "Funcionario",      "Ejecuta trámites",
                        List.of(Permission.DEPT_READ, Permission.STAFF_READ,
                                Permission.TRAMITE_READ, Permission.TRAMITE_WRITE)),
                role("SUPERVISOR",    "Supervisor",       "Supervisa trámites y reportes",
                        List.of(Permission.DEPT_READ, Permission.STAFF_READ,
                                Permission.TRAMITE_READ, Permission.TRAMITE_WRITE,
                                Permission.REPORT_READ)),
                role("SOLICITANTE",   "Solicitante",      "Inicia y consulta sus trámites",
                        List.of(Permission.TRAMITE_READ, Permission.TRAMITE_WRITE))
        ));
        log.info("Seed: 4 roles insertados.");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Usuarios ya existen, omitiendo seed.");
            return;
        }
        String adminRoleId = roleRepository.findByCode("ADMINISTRADOR")
                .orElseThrow().getId();

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("passw0rd"))
                .email("admin@laneflow.com")
                .roleId(adminRoleId)
                .build());

        log.info("Seed: usuario 'admin' creado (password: passw0rd).");
    }

    private Department dept(String code, String name, String parentId) {
        return Department.builder().code(code).name(name).parentId(parentId).build();
    }

    private Staff staff(String code, String firstName, String lastName,
                        String email, String departmentId) {
        return Staff.builder().code(code).firstName(firstName).lastName(lastName)
                .email(email).departmentId(departmentId).build();
    }

    private Role role(String code, String name, String description, List<String> permissions) {
        return Role.builder().code(code).name(name).description(description)
                .permissions(permissions).build();
    }
}
