package com.example.domain;

import java.util.List;

public class ChatResponse {
    public String question;
    public String answer;
    public List<String> retrievedContexts;
    public EvaluationMetrics metrics;

    public ChatResponse() {
    }
}