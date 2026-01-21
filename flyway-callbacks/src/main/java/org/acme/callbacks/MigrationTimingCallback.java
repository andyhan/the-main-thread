package org.acme.callbacks;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import io.quarkus.logging.Log;

public class MigrationTimingCallback implements Callback {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE || event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        if (event == Event.BEFORE_MIGRATE) {
            START_TIME.set(System.currentTimeMillis());
        }

        if (event == Event.AFTER_MIGRATE) {
            Long startTime = START_TIME.get();
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                Log.infof("Flyway migrations completed in %d ms", duration);
                START_TIME.remove();
            }
        }
    }

    @Override
    public String getCallbackName() {
        return "migration-timing-callback";
    }

    public MigrationTimingCallback() {
    }
}