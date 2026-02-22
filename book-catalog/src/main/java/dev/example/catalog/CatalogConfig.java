package dev.example.catalog;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CatalogConfig {

    @Inject
    @ConfigProperty(name = "catalog.max_results", defaultValue = "10")
    int maxResults;

    @Inject
    @ConfigProperty(name = "catalog.storage_backend")
    String storageBackend;

    @Inject
    @ConfigProperty(name = "catalog.feature.recommendations")
    Optional<Boolean> recommendationsEnabled;

    public int getMaxResults() {
        return maxResults;
    }

    public String getStorageBackend() {
        return storageBackend;
    }

    public boolean isRecommendationsEnabled() {
        return recommendationsEnabled.orElse(false);
    }
}