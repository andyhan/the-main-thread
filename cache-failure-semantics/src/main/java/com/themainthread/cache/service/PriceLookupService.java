package com.themainthread.cache.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PriceLookupService {

    private static final Logger LOG = Logger.getLogger(PriceLookupService.class);

    @Inject
    CatalogStore catalog;

    private final ConcurrentMap<String, AtomicInteger> backendCalls = new ConcurrentHashMap<>();

    @CacheResult(cacheName = "prices")
    public Uni<BigDecimal> lookupPrice(String sku) {
        backendCalls.computeIfAbsent(sku, ignored -> new AtomicInteger()).incrementAndGet();
        LOG.infof("Cache miss for %s — calling backend", sku);
        return Uni.createFrom()
                .item(() -> catalog.getPrice(sku))
                .onItem().delayIt().by(Duration.ofMillis(500));
    }

    public int backendCallsFor(String sku) {
        AtomicInteger calls = backendCalls.get(sku);
        return calls == null ? 0 : calls.get();
    }
}