package com.orgchart.resource;

import java.util.List;

import com.orgchart.dto.EmployeeHierarchyDTO;
import com.orgchart.entity.Employee;
import com.orgchart.repository.EmployeeRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @Inject
    EmployeeRepository repository;

    @GET
    public List<Employee> listAll() {
        return repository.listAll();
    }

    @GET
    @Path("/hierarchy")
    public List<EmployeeHierarchyDTO> hierarchy() {
        return repository.getHierarchyTree();
    }

    @POST
    @Transactional
    public Response create(Employee employee) {
        repository.persist(employee);
        return Response.status(201).entity(employee).build();
    }
}