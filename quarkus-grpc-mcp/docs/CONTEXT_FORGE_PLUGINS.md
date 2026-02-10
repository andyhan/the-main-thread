# Context Forge Plugins (and why they don’t register gRPC)

## What the plugins are

Context Forge’s **plugin framework** is **middleware**: plugins run at fixed **hook points** in the MCP request/response pipeline. They **inspect or modify** existing MCP traffic (tools, prompts, resources, HTTP auth); they do **not** register new backends or turn gRPC into MCP.

Reference: [Plugin Framework Specification](https://ibm.github.io/mcp-context-forge/architecture/plugins/).

## Plugin types

| Type | Description | Examples |
|------|-------------|----------|
| **Native** | In-process Python, extend base `Plugin` class | PII filter, regex filter, deny filter |
| **External** | Remote MCP servers (any language), configured with `kind: "external"` and `mcp.proto` / `mcp.url` or `mcp.script` | OPA, Cedar, LlamaGuard, OpenAI Moderation |

## Built-in / documented plugins

- **PII filter** – Detect and mask PII in requests/responses  
- **Deny filter** – Block by policy  
- **Regex filter** – Pattern-based filtering  
- **Resource filter** – Filter resource content  

## Hook points (where plugins run)

**MCP protocol:**

- `prompt_pre_fetch` / `prompt_post_fetch`
- `tool_pre_invoke` / `tool_post_invoke`
- `resource_pre_fetch` / `resource_post_fetch`

**HTTP / auth:**

- `http_pre_request`
- `http_auth_resolve_user`
- `http_auth_check_permission`
- `http_post_request`

Plugins are configured in YAML (with hot-reload), and there is an `mcpplugins` CLI for management.

## Can a plugin “register” gRPC?

**No.** Plugins are **not** used to:

- Register a gRPC (or any) backend with the gateway  
- Expose a gRPC service as MCP tools  
- Act as a gRPC→MCP adapter inside the gateway  

They only run **after** a tool/prompt/resource is already part of an MCP flow (e.g. from a registered gateway). So you **cannot** “register a plugin for gRPC” in the sense of adding your Quarkus gRPC service as an MCP backend.

For **gRPC → MCP**, you still need:

1. A **gRPC→MCP bridge** (e.g. `mcpgateway.translate --grpc ... --expose-sse`), which exposes an MCP endpoint (e.g. SSE).  
2. **Register that MCP endpoint** with Context Forge via **POST /gateways** (see [CONTEXT_FORGE_REGISTRATION.md](./CONTEXT_FORGE_REGISTRATION.md)).

Plugins are for **security, compliance, and middleware** on top of existing MCP traffic, not for adding gRPC backends.
