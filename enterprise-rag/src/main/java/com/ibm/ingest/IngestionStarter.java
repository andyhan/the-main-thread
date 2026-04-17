package com.ibm.ingest;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Kicks off background ingestion after CDI startup so Quarkus can open HTTP without waiting for Docling
 * conversion and embedding to finish.
 */
@ApplicationScoped
public class IngestionStarter {

    @Inject
    DocumentLoader documentLoader;

    void onStart(@Observes StartupEvent ignored) {
        documentLoader.startAsyncIngestion();
        Log.info("Background document ingestion scheduled (readiness will turn UP when indexing completes).");
    }
}
