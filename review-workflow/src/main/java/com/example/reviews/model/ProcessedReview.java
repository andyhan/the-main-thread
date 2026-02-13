package com.example.reviews.model;

public record ProcessedReview(
        String reviewId,
        String productId,
        int rating,
        String sentiment,
        String action,
        String finalAction,
        String approvedBy) {
}