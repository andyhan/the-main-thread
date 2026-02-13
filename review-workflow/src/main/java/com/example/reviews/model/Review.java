package com.example.reviews.model;

public record Review(
        String reviewId,
        String productId,
        int rating,
        String text) {
}