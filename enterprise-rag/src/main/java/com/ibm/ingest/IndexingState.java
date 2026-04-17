package com.ibm.ingest;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tracks whether the initial embedding ingestion has finished. Used for readiness so HTTP traffic
 * can wait until pgvector is populated (when health checks are enabled).
 */
@ApplicationScoped
public class IndexingState {

    private final AtomicBoolean indexReady = new AtomicBoolean(false);

    public boolean isIndexReady() {
        return indexReady.get();
    }

    public void setIndexReady(boolean ready) {
        indexReady.set(ready);
    }
}
