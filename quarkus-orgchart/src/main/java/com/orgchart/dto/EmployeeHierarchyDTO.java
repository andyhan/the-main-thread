package com.orgchart.dto;

import java.util.ArrayList;
import java.util.List;

public class EmployeeHierarchyDTO {

    public Long id;
    public String firstName;
    public String lastName;
    public String title;
    public String department;
    public Long managerId;
    public int level;

    public List<EmployeeHierarchyDTO> children = new ArrayList<>();

    public EmployeeHierarchyDTO(Long id,
            String firstName,
            String lastName,
            String title,
            String department,
            Long managerId,
            int level) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.department = department;
        this.managerId = managerId;
        this.level = level;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}