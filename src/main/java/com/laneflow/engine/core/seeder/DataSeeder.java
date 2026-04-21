package com.laneflow.engine.core.seeder;

import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedDepartments();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            log.info("Departamentos ya existen, omitiendo seed.");
            return;
        }

        Department ti = departmentRepository.save(dept("TI", "Tecnología de Información", null));

        List<Department> departments = List.of(
                dept("MGMT", "Gerencia General",     null),
                dept("RRHH", "Recursos Humanos",     null),
                dept("DEV",  "Desarrollo",           ti.getId()),
                dept("INF",  "Infraestructura",      ti.getId()),
                dept("FIN",  "Finanzas",             null),
                dept("OPS",  "Operaciones",          null),
                dept("LEG",  "Legal",                null),
                dept("ATC",  "Atención al Cliente",  null)
        );

        departmentRepository.saveAll(departments);
        log.info("Seed completado: {} departamentos insertados.", departments.size() + 1);
    }

    private Department dept(String code, String name, String parentId) {
        return Department.builder()
                .code(code)
                .name(name)
                .parentId(parentId)
                .build();
    }
}
