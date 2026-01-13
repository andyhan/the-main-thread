package com.example.domain;

public class ChatRequest {
    public String question;
    public String referenceAnswer; // Optional, for evaluation

    public ChatRequest() {
    }

    public ChatRequest(String question) {
        this.question = question;
    }
}