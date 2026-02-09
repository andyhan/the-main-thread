package com.orgchart.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.orgchart.dto.EmployeeHierarchyDTO;
import com.orgchart.entity.Employee;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ApplicationScoped
public class EmployeeRepository implements PanacheRepository<Employee> {

    private final EntityManager em;

    public EmployeeRepository(EntityManager em) {
        this.em = em;
    }

    public List<EmployeeHierarchyDTO> getOrganizationHierarchy() {
        String sql = """
                WITH RECURSIVE org_hierarchy AS (
                    SELECT
                        id,
                        firstName,
                        lastName,
                        title,
                        department,
                        manager_id,
                        0 AS level
                    FROM employees
                    WHERE manager_id IS NULL

                    UNION ALL

                    SELECT
                        e.id,
                        e.firstName,
                        e.lastName,
                        e.title,
                        e.department,
                        e.manager_id,
                        oh.level + 1
                    FROM employees e
                    JOIN org_hierarchy oh ON e.manager_id = oh.id
                )
                SELECT * FROM org_hierarchy
                ORDER BY level, lastName
                """;

        Query query = em.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(r -> new EmployeeHierarchyDTO(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[2],
                        (String) r[3],
                        (String) r[4],
                        r[5] != null ? ((Number) r[5]).longValue() : null,
                        ((Number) r[6]).intValue()))
                .collect(Collectors.toList());
    }

    public List<EmployeeHierarchyDTO> getHierarchyTree() {
        List<EmployeeHierarchyDTO> flat = getOrganizationHierarchy();
        Map<Long, EmployeeHierarchyDTO> index = new HashMap<>();
        List<EmployeeHierarchyDTO> roots = new ArrayList<>();

        for (EmployeeHierarchyDTO e : flat) {
            index.put(e.id, e);
        }

        for (EmployeeHierarchyDTO e : flat) {
            if (e.managerId == null) {
                roots.add(e);
            } else {
                EmployeeHierarchyDTO parent = index.get(e.managerId);
                if (parent != null) {
                    parent.children.add(e);
                }
            }
        }

        return roots;
    }
}