# IBM mcp-context-forge is a Python app; use the official image.
FROM ghcr.io/ibm/mcp-context-forge:1.0.0-BETA-2

# gRPC support is optional and not in the default image; add [grpc] extra so /grpc routes exist.
# Venv in image has no pip; bootstrap it then install gRPC extra
RUN python3 -m ensurepip --upgrade \
    && python3 -m pip install --no-cache-dir 'mcp-contextforge-gateway[grpc]' sse-starlette

# Custom gRPC→SSE bridge (BETA-2 translate --grpc does not start an HTTP server).
COPY grpc_sse_bridge.py /app/grpc_sse_bridge.py

# Gateway listens on 4444 by default; expose it (compose can map to 3000 if desired).
EXPOSE 4444

# Defaults from official image; override with env in docker-compose if needed.
# ENV HOST=0.0.0.0
# ENV PORT=4444
# ENV DATABASE_URL=sqlite:///./mcp.db