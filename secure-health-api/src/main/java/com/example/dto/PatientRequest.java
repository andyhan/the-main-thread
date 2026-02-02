package com.example.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for creating/updating a patient.
 * Includes validation rules for all fields.
 */
public class PatientRequest {

    @NotBlank(message = "First name is required")
    public String firstName;

    @NotBlank(message = "Last name is required")
    public String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    public LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    public String email;

    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{4}$", message = "SSN must be in XXX-XX-XXXX format")
    public String ssn;

    public String medicalHistory;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{4}$", message = "Phone must be in XXX-XXX-XXXX format")
    public String phoneNumber;
}