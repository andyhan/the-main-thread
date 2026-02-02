package com.example;

import java.util.List;
import java.util.stream.Collectors;

import com.example.dto.PatientDetailResponse;
import com.example.dto.PatientRequest;
import com.example.dto.PatientResponse;
import com.example.entity.Patient;
import com.example.repository.PatientRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    @Inject
    PatientRepository repository;

    /**
     * Get all patients (without sensitive data)
     */
    @GET
    public List<PatientResponse> listAll() {
        return Patient.<Patient>listAll().stream()
                .map(PatientResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get a single patient by ID (with sensitive data)
     */
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return Patient.<Patient>findByIdOptional(id)
                .map(patient -> Response.ok(PatientDetailResponse.from(patient)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Search for a patient by email
     */
    @GET
    @Path("/search")
    public Response searchByEmail(@QueryParam("email") String email) {
        if (email == null || email.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email parameter is required\"}")
                    .build();
        }

        return repository.findByEmail(email)
                .map(patient -> Response.ok(PatientResponse.from(patient)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Create a new patient
     */
    @POST
    @Transactional
    public Response create(@Valid PatientRequest request) {
        // Check for duplicate email
        if (repository.emailExists(request.email)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Email already exists\"}")
                    .build();
        }

        // Create new patient entity
        Patient patient = new Patient();
        patient.firstName = request.firstName;
        patient.lastName = request.lastName;
        patient.dateOfBirth = request.dateOfBirth;
        patient.email = request.email;
        patient.ssn = request.ssn;
        patient.medicalHistory = request.medicalHistory;
        patient.phoneNumber = request.phoneNumber;

        // Persist (encryption happens automatically via converters)
        patient.persist();

        return Response.status(Response.Status.CREATED)
                .entity(PatientResponse.from(patient))
                .build();
    }

    /**
     * Update an existing patient
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, @Valid PatientRequest request) {
        return Patient.<Patient>findByIdOptional(id)
                .map(patient -> {
                    // Check if email is being changed to an existing one
                    if (!patient.email.equals(request.email) && repository.emailExists(request.email)) {
                        return Response.status(Response.Status.CONFLICT)
                                .entity("{\"error\":\"Email already exists\"}")
                                .build();
                    }

                    // Update fields
                    patient.firstName = request.firstName;
                    patient.lastName = request.lastName;
                    patient.dateOfBirth = request.dateOfBirth;
                    patient.email = request.email;
                    patient.ssn = request.ssn;
                    patient.medicalHistory = request.medicalHistory;
                    patient.phoneNumber = request.phoneNumber;

                    return Response.ok(PatientDetailResponse.from(patient)).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Delete a patient
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = Patient.deleteById(id);

        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}