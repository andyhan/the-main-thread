# TheMainThread Academy (badge-platform)

Open Badge 2.0 style issuing demo from the article `themainthread-academy.md`.

## Run locally

Podman or Docker must be available for Dev Services (PostgreSQL, Mailpit in dev).

```bash
./mvnw quarkus:dev
```

Generate RSA keys if you do not already have them under `src/main/resources/META-INF/resources/`:

```bash
openssl genrsa -out src/main/resources/META-INF/resources/private.pem 2048
openssl rsa -in src/main/resources/META-INF/resources/private.pem \
  -pubout -out src/main/resources/META-INF/resources/public.pem
```

## Tests

```bash
./mvnw test
```

Integration tests expect Dev Services PostgreSQL (container runtime required).
