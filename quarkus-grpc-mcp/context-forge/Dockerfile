# Build ContextForge from source (tip of main) instead of using a pre-built image.
# See: https://github.com/IBM/mcp-context-forge/blob/main/Containerfile.lite
FROM python:3.12-slim-bookworm

WORKDIR /app

# pip needs git to clone the repo; install then remove to keep image smaller.
RUN apt-get update -qq && apt-get install -y --no-install-recommends git \
    && pip install --no-cache-dir \
        "git+https://github.com/IBM/mcp-context-forge.git@main#egg=mcp-contextforge-gateway[grpc]" \
    && apt-get purge -y git && apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/*

# Upstream keeps version "1.0.0-RC-1" in source; patch to show build is from main.
RUN sed -i 's/"1.0.0-RC-1"/"1.0.0-RC-1+main"/' "$(python3 -c 'import mcpgateway; print(mcpgateway.__file__)')"

# Gateway listens on 4444 by default; expose it (compose can map to 3000 if desired).
EXPOSE 4444

# For debug logging: set GUNICORN_CMD_ARGS=--log-level debug in docker-compose or at run time.
ENV HOST=0.0.0.0 \
    PORT=4444 \
    GUNICORN_CMD_ARGS=""
CMD ["gunicorn", "-w", "2", "-k", "uvicorn.workers.UvicornWorker", "-b", "0.0.0.0:4444", "mcpgateway.main:app"]
