package com.secureai.service;

import com.secureai.ai.SecureAssistant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefenseService {

    @Inject
    StruQSanitizer sanitizer;

    @Inject
    SecureAssistant assistant;

    public String processSecurely(String untrustedUserText) {
        StruQSanitizer.StructuredInput structured = sanitizer.sanitize(untrustedUserText);

        return assistant.chat(
                structured.safeXmlBlock(),
                structured.securityNonce());
    }
}