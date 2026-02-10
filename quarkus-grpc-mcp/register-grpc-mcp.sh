#!/usr/bin/env bash
# Programmatically register an MCP backend with Context Forge and create a virtual server.
#
# Two different URLs:
#   CONTEXT_FORGE_URL = where Context Forge runs (e.g. http://localhost:4444). You call the Admin API here.
#   BACKEND_URL       = the MCP server URL to register (e.g. SSE from a gRPC→MCP translate service).
#
# For Quarkus gRPC: Context Forge does NOT accept a raw gRPC address. You need a gRPC→MCP bridge
# (e.g. mcpgateway.translate --grpc quarkus-grpc:9000 --expose-sse --port 8002), then set
# BACKEND_URL to that bridge's MCP endpoint (e.g. http://host:8002/sse).
set -e

# Where Context Forge (the gateway) runs — you send POST /gateways here.
CONTEXT_FORGE_URL="${CONTEXT_FORGE_URL:-http://localhost:4444}"
# The MCP backend URL to register (e.g. SSE from translate). NOT the context-forge URL.
# Gateway runs in a container and must reach this URL:
#   Docker (Linux): BACKEND_URL=http://translate:8002/sse (service name resolves on default network).
#   Podman (Mac):   export BACKEND_URL='http://host.containers.internal:8002/sse' (single quotes avoid zsh parsing).
#   Docker:        export BACKEND_URL='http://translate:8002/sse'
BACKEND_URL="${BACKEND_URL:-}"
BACKEND_NAME="${BACKEND_NAME:-quarkus_grpc}"
SERVER_NAME="${SERVER_NAME:-quarkus_server}"
JWT_SECRET="${JWT_SECRET:-my-test-key}"
JWT_USER="${JWT_USER:-admin@example.com}"

# Resolve container name for exec (podman-compose prefix)
CONTAINER_NAME="${CONTAINER_NAME:-quarkus-grpc-mcp_context-forge_1}"

if [ -z "$BACKEND_URL" ]; then
  echo "Set BACKEND_URL to the MCP endpoint the gateway can reach (gateway runs inside a container)."
  echo "  Podman (Mac): export BACKEND_URL='http://host.containers.internal:8002/sse'"
  echo "  Docker:      export BACKEND_URL='http://translate:8002/sse'"
  echo "Context Forge runs at CONTEXT_FORGE_URL=$CONTEXT_FORGE_URL"
  exit 1
fi
echo "Context Forge (gateway): $CONTEXT_FORGE_URL"
echo "Registering backend:    $BACKEND_URL as name: $BACKEND_NAME"

# 1. Get JWT (from running container; or set MCPGATEWAY_BEARER_TOKEN env if you have it)
if [ -z "$MCPGATEWAY_BEARER_TOKEN" ]; then
  if command -v podman &>/dev/null; then
    MCPGATEWAY_BEARER_TOKEN=$(podman exec "$CONTAINER_NAME" python3 -m mcpgateway.utils.create_jwt_token \
      --username "$JWT_USER" --exp 10080 --secret "$JWT_SECRET" 2>/dev/null || true)
  fi
  if [ -z "$MCPGATEWAY_BEARER_TOKEN" ]; then
    echo "Set MCPGATEWAY_BEARER_TOKEN or run with a podman-accessible context-forge container ($CONTAINER_NAME)."
    exit 1
  fi
fi

# 2. Register gateway (backend MCP URL)
# Note: For gRPC you must first expose it as MCP (e.g. via mcpgateway.translate --grpc); then pass that SSE/HTTP URL here.
RESP=$(curl -s -X POST \
  -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$BACKEND_NAME\",\"url\":\"$BACKEND_URL\"}" \
  "$CONTEXT_FORGE_URL/gateways")
echo "POST /gateways: $RESP"

# 3. List tools and create virtual server with tool IDs
TOOLS_JSON=$(curl -s -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" "$CONTEXT_FORGE_URL/tools")
# Extract tool IDs (adapt jq path to actual API response: .tools[].id or .[].id)
ASSOC=$(echo "$TOOLS_JSON" | jq -c '[.tools[]? | .id // empty] // [.[]? | .id // empty] | if length > 0 then . else [] end' 2>/dev/null || echo "[]")

if [ "$ASSOC" = "[]" ] || [ -z "$ASSOC" ]; then
  echo "No tools found. Create a virtual server manually or ensure the backend exposes MCP tools."
  echo "Tools response: $TOOLS_JSON"
else
  SERVER_RESP=$(curl -s -X POST \
    -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$SERVER_NAME\",\"description\":\"Quarkus gRPC tools\",\"associatedTools\":$ASSOC}" \
    "$CONTEXT_FORGE_URL/servers")
  echo "POST /servers: $SERVER_RESP"
fi

echo "Done. List servers: curl -s -H \"Authorization: Bearer \$MCPGATEWAY_BEARER_TOKEN\" $CONTEXT_FORGE_URL/servers | jq"
