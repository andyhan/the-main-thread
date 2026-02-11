# MCP JSON-RPC goes to POST /message; GET /sse is only for the SSE stream.
# If you see "forwarded", rebuild and restart: podman-compose build translate && podman-compose up -d translate
resp=$(curl -s -X POST http://localhost:8002/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}')
echo "$resp" | jq . 2>/dev/null || echo "$resp"