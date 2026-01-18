package com.example.service.matching;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.example.entity.HashtagMention;
import com.example.entity.Status;
import com.example.model.MastodonStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatusConverter {

    @Inject
    StatusMatcher statusMatcher;

    /**
     * Converts a MastodonStatus to a Status entity and one or more HashtagMention entities.
     * If filtering is enabled, creates mentions only for matching hashtags.
     * If filtering is disabled, creates mentions for all hashtags in the post.
     * 
     * @param mastodonStatus the Mastodon status to convert
     * @return ConversionResult containing the Status and list of HashtagMention entities
     */
    public ConversionResult toEntities(MastodonStatus mastodonStatus) {
        // Validate required fields
        if (mastodonStatus == null || mastodonStatus.id() == null || mastodonStatus.id().isBlank()) {
            throw new IllegalArgumentException("Status or status.id() cannot be null or blank");
        }

        if (mastodonStatus.tags() == null || mastodonStatus.tags().isEmpty()) {
            return new ConversionResult(createStatus(mastodonStatus), List.of());
        }

        List<String> hashtagsToProcess;
        
        if (statusMatcher.isFilteringEnabled()) {
            // Only process hashtags that match the configured filter
            List<String> configuredHashtags = statusMatcher.getConfiguredHashtags();
            hashtagsToProcess = mastodonStatus.tags().stream()
                    .map(t -> t.name().toLowerCase())
                    .filter(configuredHashtags::contains)
                    .collect(Collectors.toList());
        } else {
            // Process all hashtags in the post
            hashtagsToProcess = mastodonStatus.tags().stream()
                    .map(t -> t.name().toLowerCase())
                    .collect(Collectors.toList());
        }

        Status status = createStatus(mastodonStatus);
        
        List<HashtagMention> mentions = hashtagsToProcess.stream()
                .filter(hashtag -> hashtag != null && !hashtag.isBlank())
                .map(hashtag -> createMention(status, hashtag))
                .collect(Collectors.toList());

        return new ConversionResult(status, mentions);
    }

    private Status createStatus(MastodonStatus mastodonStatus) {
        Status status = new Status();
        status.statusId = mastodonStatus.id();
        status.accountUsername = mastodonStatus.account() != null && mastodonStatus.account().username() != null 
                ? mastodonStatus.account().username() 
                : "unknown";
        status.instance = mastodonStatus.instance() != null ? mastodonStatus.instance() : "unknown";
        status.content = mastodonStatus.content(); // TEXT column can handle large content
        status.url = mastodonStatus.url();
        status.createdAt = mastodonStatus.createdAt() != null ? mastodonStatus.createdAt() : Instant.now();
        status.recordedAt = Instant.now();
        return status;
    }

    private HashtagMention createMention(Status status, String hashtag) {
        if (hashtag == null || hashtag.isBlank()) {
            throw new IllegalArgumentException("Hashtag cannot be null or blank");
        }
        
        HashtagMention mention = new HashtagMention();
        mention.status = status;
        mention.hashtag = hashtag;
        mention.recordedAt = Instant.now();
        return mention;
    }

    public record ConversionResult(Status status, List<HashtagMention> mentions) {
    }
}
