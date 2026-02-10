# Programmatically Registering MCP / gRPC with Context Forge

Context Forge exposes an **Admin API** (when `MCPGATEWAY_ADMIN_API_ENABLED=true`) that you can use to register backends and create virtual servers without using the UI.

## Why "Register gRPC Service" in Admin UI returns 404

The **official image** `ghcr.io/ibm/mcp-context-forge:1.0.0-BETA-2` does **not** include the REST API for gRPC service registration. The docs describe `POST /grpc` and a "gRPC Services" tab, but in BETA-2:

- There is **no** `grpc_router` in `mcpgateway/routers/` and **no** `app.include_router(..., prefix="/grpc")` in the shipped `main.py`.
- Installing the `[grpc]` extra in a custom image only adds Python dependencies (e.g. `grpcio`, `grpcio-reflection`); it does **not** add the `/grpc` routes, which exist only in newer code (e.g. on `main` or a future release).

So when the Admin UI form submits to `POST /grpc`, the server returns **404 Not Found**. The workaround is to use the **translate** bridge (see below) and register its MCP URL with the gateway.

### Why the translate container runs a custom bridge

In BETA-2, `mcpgateway.translate --grpc <target> --expose-sse --port 8002` only connects to the gRPC server and discovers services; it **does not** start an HTTP server on that port. The log says "To expose via HTTP/SSE, register this service in the gateway admin UI" but the gateway has no `/grpc` API in BETA-2. So we run a **custom script** `context-forge/grpc_sse_bridge.py` that uses the same `GrpcEndpoint` and `GrpcToMcpTranslator` from `translate_grpc`, starts uvicorn with `GET /sse` and `POST /message`, and translates MCP JSON-RPC (initialize, tools/list, tools/call) to gRPC. The translate service in `docker-compose` runs this script instead of `mcpgateway.translate --grpc`.

## Two different URLs

| Variable / concept | Meaning | Example |
|--------------------|--------|--------|
| **Context Forge URL** | Where the gateway runs. You call the Admin API here; MCP clients connect here. | `http://localhost:4444` |
| **Backend URL** | The MCP server you are *registering* with the gateway. This is **not** the context-forge URL. | `http://host:8002/sse` (e.g. from a gRPC→MCP translate service) |

So: **GATEWAY_URL** (or **BACKEND_URL** in the script) is **not** the context-forge URL. It is the URL of the MCP backend you want the gateway to proxy (e.g. an SSE endpoint from a gRPC→MCP bridge).

## 1. Programmatic registration (any MCP backend)

The documented flow is:

1. **Register a backend** (MCP server URL) via **POST /gateways** at the **Context Forge URL**.  
   The payload `url` must be a **URL to an MCP endpoint** (e.g. SSE or streamable HTTP), not a raw gRPC address.

2. **List tools** via **GET /tools** (optional; to get tool IDs).

3. **Create a virtual server** via **POST /servers** with `associatedTools` (tool IDs from step 2).

All Admin API calls require a **JWT Bearer token** in the `Authorization` header.

### Example: register an MCP server (SSE URL)

```bash
# 1. Get a JWT (dev: from container or create_jwt_token)
export MCPGATEWAY_BEARER_TOKEN=$(podman exec quarkus-grpc-mcp_context-forge_1 \
  python3 -m mcpgateway.utils.create_jwt_token \
  --username admin@example.com --exp 10080 --secret my-test-key)

# 2. Register a backend (URL must be an MCP endpoint, e.g. SSE)
curl -s -X POST \
  -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"quarkus_grpc","url":"http://localhost:8002/sse"}' \
  http://localhost:4444/gateways | jq

# 3. List tools (get IDs)
curl -s -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" \
  http://localhost:4444/tools | jq

# 4. Create virtual server with those tools (use IDs from step 3)
curl -s -X POST \
  -H "Authorization: Bearer $MCPGATEWAY_BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"quarkus_server","description":"Quarkus gRPC tools","associatedTools":["1","2","3"]}' \
  http://localhost:4444/servers | jq
```

## 2. What the official doc says: gRPC-to-MCP with reflection

The **official README** states:

- *“**gRPC-to-MCP translation** via automatic reflection-based service discovery”*
- Under “Virtualization of REST/gRPC Services”: *“**gRPC-to-MCP translation** via server reflection protocol”* and *“Automatic service discovery and method introspection”*

So the project **does** advertise built-in gRPC→MCP with reflection. The open question is **where** that is configured:

- **Option A – In the main gateway:** There may be an Admin API or Admin UI way to register a **gRPC target** (e.g. `host:port`) so the gateway uses reflection and exposes it as a virtual MCP server. The quick-start docs only show registering an **MCP URL** (e.g. SSE), not a gRPC address.
- **Option B – Via the translate module:** The **translate** module (`mcpgateway.translate`) can speak gRPC and expose MCP (e.g. `--grpc <target> --expose-sse`). The “reflection” wording may refer to that component; you would then register the **translate** MCP URL with the gateway.

**What to do:** With the gateway running, check:

1. **Swagger:** `http://localhost:4444/docs` — look at **POST /gateways** (and any “virtual server” or “grpc” endpoints) for a `type`, `grpc_target`, or similar field.
2. **Admin UI:** `http://localhost:4444/admin` — when adding a gateway/backend, see if there is an option for “gRPC” or “Reflection” and a host:port field.

If you find a direct gRPC registration (host:port), then **BACKEND_URL** in our script is not the right model for that; you’d use the gRPC address and the appropriate API shape instead.

## 3. Recommended: translate bridge, then register MCP URL

If there is **no** direct “register gRPC target” in the gateway (or it’s not obvious), the reliable path is:

1. **Run a gRPC→MCP bridge** (e.g. `mcpgateway.translate --grpc quarkus-grpc:9000 --expose-sse --port 8002`). That process uses gRPC reflection and exposes an MCP endpoint, e.g. `http://host:8002/sse`.
2. **Register that MCP URL** with Context Forge:  
   `POST http://localhost:4444/gateways` with `{"name":"quarkus_grpc","url":"http://host:8002/sse"}`.
3. Create a virtual server (POST /servers) and use it; MCP clients connect to Context Forge (e.g. `http://localhost:4444/servers/<id>/mcp`).

So: the doc’s “gRPC-to-MCP translation via reflection” might be in the **gateway** (check /docs and Admin UI) or in the **translate** module; either way, your Quarkus gRPC service (with reflection enabled) can be exposed as MCP.

## 4. Checking the live Admin API

With the gateway running:

- **Swagger UI:** `http://localhost:4444/docs` (JWT-protected)
- **ReDoc:** `http://localhost:4444/redoc`

Search for “gateway”, “grpc”, “virtual”, “server” to see if there is a dedicated gRPC registration or virtual-server-from-gRPC endpoint.

## 5. Script in this repo

- **`register-grpc-mcp.sh`** – Gets a JWT and calls **POST /gateways** and **POST /servers** with configurable names and URLs.  
- **CONTEXT_FORGE_URL** – Where Context Forge runs (default `http://localhost:4444`).  
- **BACKEND_URL** – The MCP endpoint you are registering (e.g. from the gRPC→MCP translate service: `http://host:8002/sse`). This is **not** the context-forge URL.  
- Run the script **after** you have an MCP endpoint (e.g. from the translate bridge). Set **BACKEND_URL** to a URL the **gateway container** can reach. Always use **single quotes** so zsh doesn't interpret the URL: on **Podman (Mac)** use `export BACKEND_URL='http://host.containers.internal:8002/sse'`; on Docker (Linux) use `export BACKEND_URL='http://translate:8002/sse'`.

**Troubleshooting**

- **zsh: unknown file attribute: i** — Set BACKEND_URL with single quotes: `export BACKEND_URL='http://host.containers.internal:8002/sse'`.
- **curl localhost:8002/sse returns 000 / connection refused** — The translate container may not be listening. Check `podman logs quarkus-grpc-mcp_translate_1` for errors. The compose file uses `--expose-sse` and `--host 0.0.0.0` so the server binds on all interfaces; rebuild with `podman-compose up --build -d`.
- **"Unable to connect to gateway"** — The gateway (context-forge) cannot reach the BACKEND_URL. On Podman Mac use `host.containers.internal`; on Docker use `translate`.

## References

- [Context Forge main docs](https://ibm.github.io/mcp-context-forge/)
- [Quick Start – Registering MCP tools & creating a virtual server](https://ibm.github.io/mcp-context-forge/overview/quick_start/#registering-mcp-tools-creating-a-virtual-server)
- [Architecture – Virtual Server Composition, translate module](https://ibm.github.io/mcp-context-forge/architecture/)
