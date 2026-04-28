package com.laneflow.engine.core.seeder;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.DocumentType;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
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
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedDepartments();
        seedRoles();
        seedStaff();
        seedUsers();
        seedApplicants();
    }

    // ─── Departments ──────────────────────────────────────────────────────────

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            log.info("Departments already exist, skipping.");
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
        log.info("Seed: 9 departments inserted.");
    }

    // ─── Roles ────────────────────────────────────────────────────────────────

    private void seedRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles already exist, skipping.");
            return;
        }

        List<String> funcionarioPerms = List.of(
                Permission.TRAMITE_READ,
                Permission.TRAMITE_WRITE
        );

        List<String> applicantPerms = List.of(
                Permission.TRAMITE_READ
        );

        roleRepository.saveAll(List.of(
                role("ADMINISTRADOR", "Administrador",
                        "Acceso total al sistema",
                        Permission.ALL),

                role("SUPERVISOR", "Supervisor",
                        "Supervisa tramites, flujos y reportes",
                        Permission.ALL),

                role("FUNCIONARIO", "Funcionario",
                        "Gestiona tramites, solicitantes y tareas operativas",
                        funcionarioPerms),

                role("APPLICANT", "Solicitante",
                        "Consulta sus propios tramites e historial",
                        applicantPerms)
        ));
        log.info("Seed: 4 roles inserted (ADMINISTRADOR, SUPERVISOR, FUNCIONARIO, APPLICANT).");
    }

    // ─── Staff ────────────────────────────────────────────────────────────────

    private void seedStaff() {
        if (staffRepository.count() > 0) {
            log.info("Staff already exists, skipping.");
            return;
        }
        Map<String, String> deptIds = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getCode, Department::getId));

        staffRepository.saveAll(List.of(
                // MGMT — 2
                staff("EMP-001", "Carlos",    "Mendoza",    "c.mendoza@laneflow.com",   deptIds.get("MGMT")),
                staff("EMP-010", "Ana",        "García",     "a.garcia@laneflow.com",    deptIds.get("MGMT")),
                // RRHH — 2
                staff("EMP-002", "María",      "López",      "m.lopez@laneflow.com",     deptIds.get("RRHH")),
                staff("EMP-011", "Pedro",      "Sánchez",    "p.sanchez@laneflow.com",   deptIds.get("RRHH")),
                // DEV — 2
                staff("EMP-003", "Andrés",     "Torres",     "a.torres@laneflow.com",    deptIds.get("DEV")),
                staff("EMP-012", "Carolina",   "Reyes",      "c.reyes@laneflow.com",     deptIds.get("DEV")),
                // INF — 2
                staff("EMP-013", "Felipe",     "Morales",    "f.morales@laneflow.com",   deptIds.get("INF")),
                staff("EMP-014", "Daniela",    "Cruz",       "d.cruz@laneflow.com",      deptIds.get("INF")),
                // FIN — 2
                staff("EMP-004", "Lucía",      "Ramírez",    "l.ramirez@laneflow.com",   deptIds.get("FIN")),
                staff("EMP-015", "Miguel",     "Flores",     "m.flores@laneflow.com",    deptIds.get("FIN")),
                // OPS — 2
                staff("EMP-006", "Sofía",      "Vargas",     "s.vargas@laneflow.com",    deptIds.get("OPS")),
                staff("EMP-016", "Patricia",   "Vega",       "p.vega@laneflow.com",      deptIds.get("OPS")),
                // LEG — 2
                staff("EMP-007", "Roberto",    "Castro",     "r.castro@laneflow.com",    deptIds.get("LEG")),
                staff("EMP-017", "Isabel",     "Navarro",    "i.navarro@laneflow.com",   deptIds.get("LEG")),
                // ATC — 2
                staff("EMP-005", "Diego",      "Herrera",    "d.herrera@laneflow.com",   deptIds.get("ATC")),
                staff("EMP-018", "Valentina",  "Torres",     "v.torres@laneflow.com",    deptIds.get("ATC")),
                // TI — 2
                staff("EMP-019", "Hugo",       "Blanco",     "h.blanco@laneflow.com",    deptIds.get("TI")),
                staff("EMP-020", "Natalia",    "Pérez",      "n.perez@laneflow.com",     deptIds.get("TI"))
        ));
        log.info("Seed: 18 staff members inserted (2 per department).");
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping.");
            return;
        }

        Map<String, String> roleIds = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getCode, Role::getId));
        Map<String, String> staffIds = staffRepository.findAll().stream()
                .collect(Collectors.toMap(Staff::getCode, Staff::getId));

        String pw = passwordEncoder.encode("password");

        String adminRole  = roleIds.get("ADMINISTRADOR");
        String supRole    = roleIds.get("SUPERVISOR");
        String funcRole   = roleIds.get("FUNCIONARIO");

        userRepository.saveAll(List.of(
                // ── Sistema ──────────────────────────────────────────────────
                user("admin",     "admin@laneflow.com",           null,                    adminRole, pw),

                // ── MGMT (Supervisors) ────────────────────────────────────────
                user("cmendoza",  "c.mendoza@laneflow.com",       staffIds.get("EMP-001"), supRole,   pw),
                user("agarcia",   "a.garcia@laneflow.com",        staffIds.get("EMP-010"), supRole,   pw),

                // ── RRHH (Supervisors) ────────────────────────────────────────
                user("mlopez",    "m.lopez@laneflow.com",         staffIds.get("EMP-002"), supRole,   pw),
                user("psanchez",  "p.sanchez@laneflow.com",       staffIds.get("EMP-011"), supRole,   pw),

                // ── DEV (Funcionarios) ────────────────────────────────────────
                user("atorres",   "a.torres@laneflow.com",        staffIds.get("EMP-003"), funcRole,  pw),
                user("creyes",    "c.reyes@laneflow.com",         staffIds.get("EMP-012"), funcRole,  pw),

                // ── INF (Funcionarios) ────────────────────────────────────────
                user("fmorales",  "f.morales@laneflow.com",       staffIds.get("EMP-013"), funcRole,  pw),
                user("dcruz",     "d.cruz@laneflow.com",          staffIds.get("EMP-014"), funcRole,  pw),

                // ── FIN (Funcionarios) ────────────────────────────────────────
                user("lramirez",  "l.ramirez@laneflow.com",       staffIds.get("EMP-004"), funcRole,  pw),
                user("mflores",   "m.flores@laneflow.com",        staffIds.get("EMP-015"), funcRole,  pw),

                // ── OPS (Funcionarios) ────────────────────────────────────────
                user("svargas",   "s.vargas@laneflow.com",        staffIds.get("EMP-006"), funcRole,  pw),
                user("pvega",     "p.vega@laneflow.com",          staffIds.get("EMP-016"), funcRole,  pw),

                // ── LEG (Supervisors) ─────────────────────────────────────────
                user("rcastro",   "r.castro@laneflow.com",        staffIds.get("EMP-007"), supRole,   pw),
                user("inavarro",  "i.navarro@laneflow.com",       staffIds.get("EMP-017"), supRole,   pw),

                // ── ATC (Funcionarios) ────────────────────────────────────────
                user("dherrera",  "d.herrera@laneflow.com",       staffIds.get("EMP-005"), funcRole,  pw),
                user("vtorres",   "v.torres@laneflow.com",        staffIds.get("EMP-018"), funcRole,  pw),

                // ── TI (Administradores) ──────────────────────────────────────
                user("hblanco",   "h.blanco@laneflow.com",        staffIds.get("EMP-019"), adminRole, pw),
                user("nperez",    "n.perez@laneflow.com",         staffIds.get("EMP-020"), adminRole, pw)
        ));
        log.info("Seed: 19 users inserted (password: password).");
    }

    // ─── Applicants + their portal users ─────────────────────────────────────

    private void seedApplicants() {
        if (applicantRepository.count() > 0) {
            log.info("Applicants already exist, skipping.");
            return;
        }

        String applicantRoleId = roleRepository.findByCode("APPLICANT")
                .map(Role::getId)
                .orElse(null);

        // Natural persons
        createApplicantWithUser(
                naturalApplicant("CI", "1001001", "Juan", "Perez",
                        "juan.perez@example.com", "70010001"),
                applicantRoleId);

        createApplicantWithUser(
                naturalApplicant("CI", "1001002", "Ana", "Rojas",
                        "ana.rojas@example.com", "70010002"),
                applicantRoleId);

        createApplicantWithUser(
                naturalApplicant("PASSPORT", "P-1001003", "Luis", "Fernandez",
                        "luis.fernandez@example.com", "70010003"),
                applicantRoleId);

        createApplicantWithUser(
                naturalApplicant("CI", "1001004", "Carmen", "Villanueva",
                        "carmen.villanueva@example.com", "70010004"),
                applicantRoleId);

        createApplicantWithUser(
                naturalApplicant("CI", "1001005", "Jorge", "Mamani",
                        "jorge.mamani@example.com", "70010005"),
                applicantRoleId);

        // Legal entities
        createApplicantWithUser(
                legalApplicant("NIT", "2002002001", "Constructora Andina SRL",
                        "Marta Salinas", "contacto@andina.example.com", "71020001"),
                applicantRoleId);

        createApplicantWithUser(
                legalApplicant("NIT", "2002002002", "Servicios Delta SA",
                        "Roberto Vargas", "contacto@delta.example.com", "71020002"),
                applicantRoleId);

        createApplicantWithUser(
                legalApplicant("NIT", "2002002003", "Importaciones Norte SRL",
                        "Elena Quispe", "contacto@norte.example.com", "71020003"),
                applicantRoleId);

        createApplicantWithUser(
                legalApplicant("NIT", "2002002004", "Tecnología Sur SA",
                        "Marco Ríos", "contacto@sur.example.com", "71020004"),
                applicantRoleId);

        log.info("Seed: 9 applicants inserted with linked portal users.");
        log.info("Applicant passwords = document number (lowercase).");
    }

    /**
     * Saves the applicant and creates a linked User for mobile/portal access.
     * Username = documentNumber.toLowerCase(), password = documentNumber (raw).
     */
    private void createApplicantWithUser(Applicant applicant, String applicantRoleId) {
        Applicant saved = applicantRepository.save(applicant);

        String username = saved.getDocumentNumber().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User.UserBuilder builder = User.builder()
                .username(username)
                .password(passwordEncoder.encode(saved.getDocumentNumber()))
                .applicantId(saved.getId())
                .roleId(applicantRoleId);

        if (saved.getEmail() != null) {
            builder.email(saved.getEmail());
        }

        userRepository.save(builder.build());
    }

    // ─── Builders ─────────────────────────────────────────────────────────────

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

    private User user(String username, String email, String staffId,
                      String roleId, String encodedPassword) {
        return User.builder()
                .username(username)
                .password(encodedPassword)
                .email(email)
                .staffId(staffId)
                .roleId(roleId)
                .build();
    }

    private Applicant naturalApplicant(String documentType, String documentNumber,
                                       String firstName, String lastName,
                                       String email, String phone) {
        return Applicant.builder()
                .type(ApplicantType.NATURAL_PERSON)
                .documentType(DocumentType.valueOf(documentType))
                .documentNumber(documentNumber)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .address("Sin dirección registrada")
                .build();
    }

    private Applicant legalApplicant(String documentType, String documentNumber,
                                     String businessName, String legalRepresentative,
                                     String email, String phone) {
        return Applicant.builder()
                .type(ApplicantType.LEGAL_ENTITY)
                .documentType(DocumentType.valueOf(documentType))
                .documentNumber(documentNumber)
                .businessName(businessName)
                .legalRepresentative(legalRepresentative)
                .email(email)
                .phone(phone)
                .address("Sin dirección registrada")
                .build();
    }
}
