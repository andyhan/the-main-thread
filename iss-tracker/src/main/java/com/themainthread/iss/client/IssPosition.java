package com.themainthread.iss.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssPosition(
        @JsonProperty("latitude") String latitude,
        @JsonProperty("longitude") String longitude) {
    public double latDouble() {
        return Double.parseDouble(latitude);
    }

    public double lonDouble() {
        return Double.parseDouble(longitude);
    }
}