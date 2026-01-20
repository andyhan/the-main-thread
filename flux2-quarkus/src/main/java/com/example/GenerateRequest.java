package com.example;

public record GenerateRequest(
        String prompt,
        Integer width,
        Integer height,
        Integer steps) {

    public GenerateRequest {
        // Default values
        if (width == null) {
            width = 512;
        }
        if (height == null) {
            height = 512;
        }
        if (steps == null) {
            steps = 20;
        }

        // Validation
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        if (width < 256 || width > 2048) {
            throw new IllegalArgumentException("Width must be between 256 and 2048");
        }

        if (height < 256 || height > 2048) {
            throw new IllegalArgumentException("Height must be between 256 and 2048");
        }

        if (steps < 1 || steps > 100) {
            throw new IllegalArgumentException("Steps must be between 1 and 100");
        }
    }
}