package com.example.service.matching;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.example.model.MastodonStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatusMatcher {

    private static final Logger LOG = Logger.getLogger(StatusMatcher.class);

    private final Optional<List<String>> configuredHashtags;

    @Inject
    public StatusMatcher(@ConfigProperty(name = "app.hashtags", defaultValue = "") Optional<List<String>> hashtagsConfig) {
        // Normalize hashtags to lowercase for comparison, if configured
        // Handle both empty string and empty list cases
        this.configuredHashtags = hashtagsConfig
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> list.stream()
                        .map(String::toLowerCase)
                        .filter(s -> s != null && !s.isBlank())
                        .toList())
                .filter(list -> !list.isEmpty());
        
        if (configuredHashtags.isPresent()) {
            LOG.infof("Monitoring specific hashtags: %s", configuredHashtags.get());
        } else {
            LOG.info("No hashtags configured - collecting ALL hashtags from posts");
        }
    }

    public boolean matches(MastodonStatus status) {
        // Skip posts with zero hashtags
        if (status.tags() == null || status.tags().isEmpty()) {
            LOG.debugf("Status has no tags: id=%s", status.id());
            return false;
        }
        
        // If no hashtags configured, match any post with at least one hashtag
        if (configuredHashtags.isEmpty()) {
            LOG.debugf("Status match (collect all): id=%s, tags=%s", 
                    status.id(), 
                    status.tags().stream().map(t -> t.name()).toList());
            return true;
        }
        
        // Otherwise, only match posts containing configured hashtags
        boolean matches = status.tags().stream()
                .map(t -> t.name().toLowerCase())
                .anyMatch(h -> configuredHashtags.get().contains(h));
        
        LOG.debugf("Status match check: id=%s, tags=%s, configured=%s, matches=%s", 
                status.id(), 
                status.tags().stream().map(t -> t.name()).toList(),
                configuredHashtags.get(),
                matches);
        
        return matches;
    }

    public boolean isFilteringEnabled() {
        return configuredHashtags.isPresent();
    }

    public List<String> getConfiguredHashtags() {
        return configuredHashtags.orElse(List.of());
    }
}
