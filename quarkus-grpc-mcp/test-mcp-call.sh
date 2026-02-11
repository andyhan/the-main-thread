#!/bin/bash
# Call the SayHello gRPC method via the MCP bridge (port 8002).
# Tool name must be the full gRPC name: package.Service.Method (see tools/list).
resp=$(curl -s -X POST http://localhost:8002/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"greeting.GreetingService.SayHello","arguments":{"name":"MCP"}}}')
echo "$resp" | jq . 2>/dev/null || echo "$resp"
