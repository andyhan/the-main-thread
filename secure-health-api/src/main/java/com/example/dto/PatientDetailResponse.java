package com.example.dto;

import java.time.LocalDate;

import com.example.entity.Patient;

/**
 * DTO for returning complete patient data including sensitive fields.
 * Used when fetching a single patient by ID.
 */
public class PatientDetailResponse {
    public Long id;
    public String firstName;
    public String lastName;
    public LocalDate dateOfBirth;
    public String email;
    public String phoneNumber;

    // Sensitive fields (only in detail view)
    public String ssn;
    public String medicalHistory;

    public static PatientDetailResponse from(Patient patient) {
        PatientDetailResponse response = new PatientDetailResponse();
        response.id = patient.id;
        response.firstName = patient.firstName;
        response.lastName = patient.lastName;
        response.dateOfBirth = patient.dateOfBirth;
        response.email = patient.email;
        response.phoneNumber = patient.phoneNumber;
        response.ssn = patient.ssn;
        response.medicalHistory = patient.medicalHistory;
        return response;
    }
}