CREATE TABLE earner (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE badge_template (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    description  TEXT NOT NULL,
    criteria     TEXT NOT NULL,
    image_url    VARCHAR(512) NOT NULL,
    skills       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accredited_partner (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    webhook_secret VARCHAR(255) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE partner_badge_template (
    partner_id        UUID NOT NULL REFERENCES accredited_partner(id),
    course_id         VARCHAR(255) NOT NULL,
    badge_template_id UUID NOT NULL REFERENCES badge_template(id),
    PRIMARY KEY (partner_id, course_id)
);

CREATE TABLE badge_assertion (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    earner_id     UUID NOT NULL REFERENCES earner(id),
    template_id   UUID NOT NULL REFERENCES badge_template(id),
    issued_on     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ,
    revoked       BOOLEAN NOT NULL DEFAULT false,
    revoke_reason VARCHAR(512),
    signed_token  TEXT NOT NULL,
    salt          VARCHAR(64) NOT NULL
);

CREATE TABLE webhook_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id      UUID NOT NULL REFERENCES accredited_partner(id),
    idempotency_key VARCHAR(255) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    error           TEXT,
    UNIQUE (partner_id, idempotency_key)
);

CREATE INDEX idx_assertion_earner ON badge_assertion(earner_id);
CREATE INDEX idx_assertion_template ON badge_assertion(template_id);
CREATE INDEX idx_webhook_status ON webhook_event(status);
