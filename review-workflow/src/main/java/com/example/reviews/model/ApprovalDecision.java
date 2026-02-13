package com.example.reviews.model;

public record ApprovalDecision(
        String reviewId,
        boolean approved,
        String approver) {
}
