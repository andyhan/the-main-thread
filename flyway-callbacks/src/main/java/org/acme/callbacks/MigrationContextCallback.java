package org.acme.callbacks;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import io.quarkus.logging.Log;

public class MigrationContextCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_EACH_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        // Get the current migration info
        MigrationInfo info = context.getMigrationInfo();

        if (info != null) {
            Log.infof("Running migration: %s", info.getDescription());
            Log.infof("Version: %s", info.getVersion());
            Log.infof("Script: %s", info.getScript());
        }

        // Access the database connection
        // Connection connection = context.getConnection();

        // Get Flyway configuration
        // Configuration config = context.getConfiguration();

    }

    @Override
    public String getCallbackName() {
        return "migration-context-callback";
    }

    public MigrationContextCallback() {
    }

}
