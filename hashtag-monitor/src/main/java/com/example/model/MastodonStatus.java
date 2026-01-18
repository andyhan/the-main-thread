package com.example.model;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MastodonStatus(
        String id,
        String url,
        @JsonProperty("created_at") Instant createdAt,
        Account account,
        String content,
        List<Tag> tags) {

    public record Account(String username, String acct) {
    }

    public record Tag(String name) {
    }

    public String instance() {
        return acctContainsInstance()
                ? account.acct().split("@", 2)[1]
                : "unknown";
    }

    private boolean acctContainsInstance() {
        return account != null && account.acct() != null && account.acct().contains("@");
    }
}
