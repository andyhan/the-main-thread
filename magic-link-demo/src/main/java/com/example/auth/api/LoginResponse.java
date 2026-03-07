package com.example.auth.api;

public class LoginResponse {

    public String token;
    public String email;
    public String displayName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email, String displayName) {
        this.token = token;
        this.email = email;
        this.displayName = displayName;
    }
}