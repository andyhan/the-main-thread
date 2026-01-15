package com.secure.text.dto;

import com.secure.text.validation.SafeText;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDto {

    @NotBlank
    @Size(min = 3, max = 20)
    @SafeText(message = "Username contains invisible or control characters")
    public String username;

    @NotBlank
    @Email
    public String email;

    @NotBlank
    @Size(max = 50)
    @SafeText
    public String bio;
}