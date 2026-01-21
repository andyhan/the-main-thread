CREATE TABLE IF NOT EXISTS migration_audit (
    event TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO migration_audit (event)
VALUES ('Migration starting');