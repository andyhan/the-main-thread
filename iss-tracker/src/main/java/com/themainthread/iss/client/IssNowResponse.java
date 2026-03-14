package com.themainthread.iss.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssNowResponse(
        String message,
        long timestamp,
        @JsonProperty("iss_position") IssPosition issPosition) {
}