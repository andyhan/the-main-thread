CREATE TABLE person (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    registered_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE SEQUENCE Person_SEQ start with 1 increment by 50;