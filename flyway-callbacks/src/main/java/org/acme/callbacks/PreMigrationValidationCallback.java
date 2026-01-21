package org.acme.callbacks;

import org.eclipse.microprofile.config.ConfigProvider;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import io.quarkus.logging.Log;

public class PreMigrationValidationCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        String profile = ConfigProvider.getConfig().getValue("quarkus.profile", String.class);
        boolean destructiveAllowed = ConfigProvider.getConfig()
                .getOptionalValue("migration.destructive.allowed", Boolean.class).orElse(false);

        Log.infof("Profile: %s, Destructive allowed: %b", profile, destructiveAllowed);

        if ("prod".equals(profile) && !destructiveAllowed) {
            throw new FlywayException(
                    "Refusing to run migrations in production without explicit approval");
        }
    }

    @Override
    public String getCallbackName() {
        return "pre-migration-validation";
    }

    public PreMigrationValidationCallback() {
    }

}
