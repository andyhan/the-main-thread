package com.noir;

public record OrderRequest(String orderId, String userId, double amount) {
}
