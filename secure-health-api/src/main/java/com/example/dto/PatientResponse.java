package com.example.dto;

import java.time.LocalDate;

import com.example.entity.Patient;

/**
 * DTO for returning patient data in list views.
 * Excludes sensitive fields like SSN and medical history.
 */
public class PatientResponse {
    public Long id;
    public String firstName;
    public String lastName;
    public LocalDate dateOfBirth;
    public String email;
    public String phoneNumber;

    public static PatientResponse from(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.id = patient.id;
        response.firstName = patient.firstName;
        response.lastName = patient.lastName;
        response.dateOfBirth = patient.dateOfBirth;
        response.email = patient.email;
        response.phoneNumber = patient.phoneNumber;
        return response;
    }
}