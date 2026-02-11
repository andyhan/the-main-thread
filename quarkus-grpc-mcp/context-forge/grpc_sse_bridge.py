#!/usr/bin/env python3
"""
gRPC→SSE bridge: exposes a gRPC server as MCP over HTTP /sse and /message.

BETA-2's mcpgateway.translate --grpc only discovers services and does not start
an HTTP server. This script starts uvicorn with /sse and /message that translate
MCP JSON-RPC (initialize, tools/list, tools/call) to gRPC and back.
"""
import asyncio
import json
import os
import uuid
from typing import Any, Dict, List

# Protobuf 5+ removed MessageFactory.GetPrototype; translate_grpc still uses it.
from google.protobuf import message_factory as _mf
if not hasattr(_mf.MessageFactory, "GetPrototype"):
    _mf.MessageFactory.GetPrototype = lambda self, descriptor: _mf.GetMessageClass(descriptor)

# Protobuf 5+ renamed including_default_value_fields -> always_print_fields_with_no_presence.
from google.protobuf import json_format as _jf
_MessageToDict_orig = _jf.MessageToDict
def _MessageToDict(message, **kwargs):
    if "including_default_value_fields" in kwargs:
        kwargs.setdefault("always_print_fields_with_no_presence", kwargs.pop("including_default_value_fields"))
    return _MessageToDict_orig(message, **kwargs)
_jf.MessageToDict = _MessageToDict

from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse, PlainTextResponse
from sse_starlette.sse import EventSourceResponse
import uvicorn

# Use mcpgateway.translate_grpc from the image (has [grpc] installed)
from mcpgateway.translate_grpc import GrpcEndpoint, GrpcToMcpTranslator

GRPC_TARGET = os.environ.get("GRPC_TARGET", "localhost:9000")
PORT = int(os.environ.get("PORT", "8002"))
HOST = os.environ.get("HOST", "0.0.0.0")
KEEP_ALIVE = 30.0

# One queue per SSE session would be ideal; we use a single global queue
# so the first GET /sse client receives all responses (sufficient for gateway).
response_queue: asyncio.Queue[str] = asyncio.Queue(maxsize=1024)


def build_mcp_tools(translator: GrpcToMcpTranslator) -> List[Dict[str, Any]]:
    """All gRPC methods as MCP tools (name, description, inputSchema)."""
    tools: List[Dict[str, Any]] = []
    for svc in translator._endpoint.get_services():
        if "reflection" in svc.lower() or "Health" in svc:
            continue
        tools.extend(translator.grpc_methods_to_mcp_tools(svc))
    return tools


async def handle_jsonrpc(
    payload: dict,
    endpoint: GrpcEndpoint,
    translator: GrpcToMcpTranslator,
) -> dict:
    """Handle one MCP JSON-RPC request; return the response object (no id yet)."""
    method = payload.get("method")
    params = payload.get("params") or {}
    req_id = payload.get("id")

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "serverInfo": {"name": "grpc-sse-bridge", "version": "0.1.0"},
                "capabilities": {"tools": {}},
            },
        }

    if method == "tools/list":
        tools = build_mcp_tools(translator)
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {"tools": [{"name": t["name"], "description": t.get("description", ""), "inputSchema": t.get("inputSchema", {})} for t in tools]},
        }

    if method == "tools/call":
        name = params.get("name") or ""
        arguments = params.get("arguments") or {}
        if "." not in name:
            return {"jsonrpc": "2.0", "id": req_id, "error": {"code": -32602, "message": f"Invalid tool name: {name}"}}
        parts = name.rsplit(".", 1)
        service, method_name = parts[0], parts[1]
        try:
            result = await endpoint.invoke(service, method_name, arguments)
            text = json.dumps(result, default=str)
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{"type": "text", "text": text}],
                    "isError": False,
                },
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{"type": "text", "text": str(e)}],
                    "isError": True,
                },
            }

    return {"jsonrpc": "2.0", "id": req_id, "error": {"code": -32601, "message": f"Method not found: {method}"}}


def create_app(endpoint: GrpcEndpoint, translator: GrpcToMcpTranslator) -> FastAPI:
    app = FastAPI()

    @app.get("/sse")
    async def get_sse(request: Request):
        session_id = uuid.uuid4().hex
        base = str(request.base_url).rstrip("/")
        message_url = f"{base}/message?session_id={session_id}"

        async def event_gen():
            yield {"event": "endpoint", "data": message_url, "retry": int(KEEP_ALIVE * 1000)}
            yield {"event": "keepalive", "data": "{}", "retry": int(KEEP_ALIVE * 1000)}
            while True:
                try:
                    if await request.is_disconnected():
                        break
                    msg = await asyncio.wait_for(response_queue.get(), timeout=KEEP_ALIVE)
                    yield {"event": "message", "data": msg}
                except asyncio.TimeoutError:
                    yield {"event": "keepalive", "data": "{}", "retry": int(KEEP_ALIVE * 1000)}

        return EventSourceResponse(
            event_gen(),
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no"},
        )

    @app.post("/message")
    async def post_message(raw: Request):
        body = await raw.body()
        try:
            payload = json.loads(body)
        except json.JSONDecodeError as e:
            return PlainTextResponse(f"Invalid JSON: {e}", status_code=status.HTTP_400_BAD_REQUEST)
        response = await handle_jsonrpc(payload, endpoint, translator)
        line = json.dumps(response) + "\n"
        try:
            response_queue.put_nowait(line)
        except asyncio.QueueFull:
            pass
        # Return JSON-RPC response in body so direct callers (e.g. curl) get the result.
        return JSONResponse(content=response)

    @app.get("/healthz")
    async def health():
        return PlainTextResponse("ok")

    return app


async def main():
    endpoint = GrpcEndpoint(target=GRPC_TARGET, reflection_enabled=True)
    await endpoint.start()
    translator = GrpcToMcpTranslator(endpoint)
    app = create_app(endpoint, translator)
    config = uvicorn.Config(app, host=HOST, port=PORT, log_level="info")
    server = uvicorn.Server(config)
    await server.serve()


if __name__ == "__main__":
    asyncio.run(main())
