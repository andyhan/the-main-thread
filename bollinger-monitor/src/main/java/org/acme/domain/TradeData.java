package org.acme.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeData(
        @JsonProperty("p") double price,
        @JsonProperty("T") long timestamp) {
}