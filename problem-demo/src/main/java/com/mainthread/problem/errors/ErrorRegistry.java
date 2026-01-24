package com.mainthread.problem.errors;

import java.net.URI;

public enum ErrorRegistry {

    INSUFFICIENT_FUNDS("insufficient-funds", "Not enough credit"),
    ACCOUNT_LOCKED("account-locked", "Account is locked");

    private static final String BASE_URI = "http://localhost:8080/#";

    private final String key;
    private final String defaultTitle;

    ErrorRegistry(String key, String defaultTitle) {
        this.key = key;
        this.defaultTitle = defaultTitle;
    }

    public URI getType() {
        return URI.create(BASE_URI + key);
    }

    public String getTitle() {
        return defaultTitle;
    }
}
