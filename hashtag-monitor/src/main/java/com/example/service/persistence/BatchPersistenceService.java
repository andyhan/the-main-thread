package com.example.service.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import com.example.entity.HashtagMention;
import com.example.entity.Status;
import com.example.service.buffer.MentionBuffer;
import com.example.web.HashtagStatsEventEmitter;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BatchPersistenceService {

    private static final Logger LOG = Logger.getLogger(BatchPersistenceService.class);

    @Inject
    MentionBuffer buffer;

    @Inject
    HashtagStatsEventEmitter eventEmitter;

    @Scheduled(every = "${app.buffer.flush-interval-seconds}")
    @Transactional
    void flush() {
        int bufferSize = buffer.size();
        LOG.debugf("Flushing buffer: size=%d", bufferSize);
        
        if (bufferSize == 0) {
            return;
        }

        // Drain buffer before transaction to avoid holding items during DB operations
        List<MentionBuffer.BufferedMention> batch = buffer.drain();
        
        if (batch.isEmpty()) {
            return;
        }

        // Filter out invalid items
        List<MentionBuffer.BufferedMention> validBatch = batch.stream()
                .filter(bm -> bm != null 
                        && bm.status != null 
                        && bm.status.statusId != null && !bm.status.statusId.isBlank()
                        && bm.mention != null
                        && bm.mention.hashtag != null && !bm.mention.hashtag.isBlank()
                        && bm.mention.recordedAt != null)
                .collect(Collectors.toList());
        
        int invalidCount = batch.size() - validBatch.size();
        if (invalidCount > 0) {
            LOG.warnf("Filtered out %d invalid mentions (null or blank required fields)", invalidCount);
        }
        
        if (validBatch.isEmpty()) {
            return;
        }

        try {
            // Step 1: Persist or find existing Status entities
            Map<String, Status> statusMap = persistOrFindStatuses(validBatch);
            
            // Step 2: Link mentions to their statuses and check for duplicates
            Set<String> existingCombinations = findExistingMentions(validBatch, statusMap);
            
            // Step 3: Create mentions linked to statuses
            List<HashtagMention> toPersist = validBatch.stream()
                    .filter(bm -> {
                        String statusId = bm.status.statusId;
                        String hashtag = bm.mention.hashtag;
                        String key = statusId + "|" + hashtag;
                        return !existingCombinations.contains(key);
                    })
                    .map(bm -> {
                        HashtagMention mention = new HashtagMention();
                        mention.status = statusMap.get(bm.status.statusId);
                        mention.hashtag = bm.mention.hashtag;
                        mention.recordedAt = bm.mention.recordedAt;
                        return mention;
                    })
                    .collect(Collectors.toList());

            int duplicates = validBatch.size() - toPersist.size();
            if (duplicates > 0) {
                LOG.debugf("Skipping %d duplicate mentions", duplicates);
            }

            if (!toPersist.isEmpty()) {
                HashtagMention.persist(toPersist);
                LOG.infof("Persisted %d mentions to database (skipped %d duplicates)", 
                        toPersist.size(), duplicates);
                // Notify clients of data update
                eventEmitter.emitUpdate();
            }
        } catch (PersistenceException e) {
            // Handle constraint violations gracefully
            if (e.getCause() instanceof ConstraintViolationException) {
                LOG.warnf(e, "Constraint violation during batch persist - some mentions may be duplicates");
                // Try persisting individually to identify which ones fail
                persistIndividually(validBatch);
            } else {
                LOG.errorf(e, "Failed to persist batch - %d mentions lost", validBatch.size());
                throw e; // Re-throw to rollback transaction
            }
        }
    }

    private Map<String, Status> persistOrFindStatuses(List<MentionBuffer.BufferedMention> batch) {
        // Get unique status IDs
        Set<String> statusIds = batch.stream()
                .map(bm -> bm.status.statusId)
                .collect(Collectors.toSet());

        // Find existing statuses
        Map<String, Status> statusMap = new HashMap<>();
        if (!statusIds.isEmpty()) {
            List<Status> existing = Status.find("statusId in ?1", statusIds).list();
            existing.forEach(s -> statusMap.put(s.statusId, s));
        }

        // Persist new statuses
        List<Status> newStatuses = batch.stream()
                .map(bm -> bm.status)
                .filter(s -> !statusMap.containsKey(s.statusId))
                .distinct()
                .collect(Collectors.toList());

        if (!newStatuses.isEmpty()) {
            Status.persist(newStatuses);
            newStatuses.forEach(s -> statusMap.put(s.statusId, s));
        }

        return statusMap;
    }

    private Set<String> findExistingMentions(List<MentionBuffer.BufferedMention> batch, Map<String, Status> statusMap) {
        // Get status IDs that exist in the map
        List<Long> statusEntityIds = batch.stream()
                .map(bm -> statusMap.get(bm.status.statusId))
                .filter(s -> s != null && s.id != null)
                .map(s -> s.id)
                .distinct()
                .collect(Collectors.toList());

        if (statusEntityIds.isEmpty()) {
            return Set.of();
        }

        // Find existing mentions by status entity ID and hashtag
        return HashtagMention.<HashtagMention>find("status.id in ?1", statusEntityIds)
                .list()
                .stream()
                .map(m -> m.status.statusId + "|" + m.hashtag)
                .collect(Collectors.toSet());
    }

    @Transactional
    void persistIndividually(List<MentionBuffer.BufferedMention> batch) {
        int success = 0;
        int duplicates = 0;
        int errors = 0;
        int invalid = 0;

        for (MentionBuffer.BufferedMention bm : batch) {
            // Skip invalid mentions
            if (bm == null || bm.status == null || bm.status.statusId == null || bm.status.statusId.isBlank()
                    || bm.mention == null || bm.mention.hashtag == null || bm.mention.hashtag.isBlank()) {
                invalid++;
                continue;
            }
            
            try {
                // Find or persist status
                Status status = Status.find("statusId", bm.status.statusId).firstResult();
                if (status == null) {
                    bm.status.persist();
                    status = bm.status;
                }
                
                // Check if mention already exists
                if (HashtagMention.find("status.id = ?1 AND hashtag = ?2", 
                        status.id, bm.mention.hashtag).firstResult() == null) {
                    HashtagMention mention = new HashtagMention();
                    mention.status = status;
                    mention.hashtag = bm.mention.hashtag;
                    mention.recordedAt = bm.mention.recordedAt;
                    mention.persist();
                    success++;
                } else {
                    duplicates++;
                }
            } catch (PersistenceException e) {
                if (e.getCause() instanceof ConstraintViolationException) {
                    duplicates++;
                } else {
                    errors++;
                    LOG.warnf(e, "Failed to persist mention: statusId=%s, hashtag=%s", 
                            bm.status.statusId, bm.mention.hashtag);
                }
            }
        }
        
        if (invalid > 0) {
            LOG.warnf("Skipped %d invalid mentions during individual persist", invalid);
        }

        LOG.infof("Individual persist: %d succeeded, %d duplicates, %d errors", 
                success, duplicates, errors);
    }
}
