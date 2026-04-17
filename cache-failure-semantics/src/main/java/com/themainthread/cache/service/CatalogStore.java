package com.themainthread.cache.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CatalogStore {

    private final ConcurrentMap<String, BigDecimal> prices = new ConcurrentHashMap<>(Map.of(
            "SKU-001", new BigDecimal("29.99"),
            "SKU-002", new BigDecimal("49.99"),
            "SKU-003", new BigDecimal("9.99")));

    public BigDecimal getPrice(String sku) {
        return prices.getOrDefault(sku, BigDecimal.ZERO);
    }

    public void updatePrice(String sku, BigDecimal newPrice) {
        prices.put(sku, newPrice);
    }
}