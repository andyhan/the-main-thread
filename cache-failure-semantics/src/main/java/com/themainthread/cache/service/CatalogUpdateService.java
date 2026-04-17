package com.themainthread.cache.service;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;

@ApplicationScoped
public class CatalogUpdateService {

    private static final Logger LOG = Logger.getLogger(CatalogUpdateService.class);

    @Inject
    CatalogStore catalog;

    @Inject
    CacheManager cacheManager;

    @CacheInvalidateAll(cacheName = "prices")
    public void updatePrice(String sku, BigDecimal newPrice, boolean simulateFailure) {
        LOG.infof("Writing new price %s for %s", newPrice, sku);
        catalog.updatePrice(sku, newPrice);
        if (simulateFailure) {
            throw new RuntimeException("Simulated failure after writing " + sku);
        }
        LOG.infof("Price updated successfully for %s", sku);
    }

    public void updatePriceWithForcedInvalidation(String sku, BigDecimal newPrice,
            boolean simulateFailure) {
        try {
            LOG.infof("Writing new price %s for %s (forced invalidation path)", newPrice, sku);
            catalog.updatePrice(sku, newPrice);
            if (simulateFailure) {
                throw new RuntimeException("Simulated failure after writing " + sku);
            }
            LOG.infof("Price updated successfully for %s", sku);
        } finally {
            Optional<Cache> cache = cacheManager.getCache("prices");
            if (cache.isPresent()) {
                cache.get().invalidateAll().await().indefinitely();
                LOG.info("Programmatic cache invalidation completed");
            }
        }
    }
}