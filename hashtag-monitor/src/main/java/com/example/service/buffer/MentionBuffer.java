package com.example.service.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.example.entity.HashtagMention;
import com.example.entity.Status;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MentionBuffer {

    private static final Logger LOG = Logger.getLogger(MentionBuffer.class);

    public static class BufferedMention {
        public final Status status;
        public final HashtagMention mention;

        public BufferedMention(Status status, HashtagMention mention) {
            this.status = status;
            this.mention = mention;
        }
    }

    private final Queue<BufferedMention> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger droppedCount = new AtomicInteger(0);

    @ConfigProperty(name = "app.buffer.max-size")
    int maxSize;

    /**
     * Adds a mention with its associated status to the buffer if there's space.
     * @param status the status entity
     * @param mention the mention to add
     * @return true if added, false if buffer was full
     */
    public boolean add(Status status, HashtagMention mention) {
        // Use atomic check-and-add to prevent race conditions
        if (buffer.size() >= maxSize) {
            int dropped = droppedCount.incrementAndGet();
            if (dropped % 100 == 0) {
                LOG.warnf("Buffer full, dropped %d mentions so far (statusId=%s)", 
                        dropped, status != null ? status.statusId : "null");
            }
            return false;
        }
        buffer.offer(new BufferedMention(status, mention));
        return true;
    }

    /**
     * Drains all items from the buffer.
     * @return list of all mentions in the buffer with their associated statuses
     */
    public List<BufferedMention> drain() {
        List<BufferedMention> batch = new ArrayList<>();
        BufferedMention m;
        while ((m = buffer.poll()) != null) {
            batch.add(m);
        }
        return batch;
    }

    /**
     * Drains up to the specified number of items from the buffer.
     * @param maxItems maximum number of items to drain
     * @return list of mentions with statuses (may be less than maxItems if buffer is smaller)
     */
    public List<BufferedMention> drain(int maxItems) {
        List<BufferedMention> batch = new ArrayList<>();
        BufferedMention m;
        int count = 0;
        while (count < maxItems && (m = buffer.poll()) != null) {
            batch.add(m);
            count++;
        }
        return batch;
    }

    public int size() {
        return buffer.size();
    }

    public int getDroppedCount() {
        return droppedCount.get();
    }

    public void resetDroppedCount() {
        droppedCount.set(0);
    }
}
