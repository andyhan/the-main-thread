package com.example.mcp;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
class McpSmokeTest {

    /**
     * MCP Streamable HTTP requires Accept to include both application/json and
     * text/event-stream.
     */
    private static final String MCP_ACCEPT = "application/json, text/event-stream";

    @Test
    void toolsListShouldIncludeOurTools() {
        String initialize = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-03-26",
                    "capabilities": {},
                    "clientInfo": { "name": "JUnit", "version": "1.0" }
                  }
                }
                """;

        Response initResponse = given()
                .accept(MCP_ACCEPT)
                .contentType("application/json")
                .body(initialize)
                .post("/mcp");
        initResponse.then()
                .statusCode(200)
                .body(containsString("serverInfo"))
                .body(containsString("dev-toolkit-mcp"));
        String sessionId = initResponse.getHeader("Mcp-Session-Id");

        String toolsList = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list"
                }
                """;

        given()
                .accept(MCP_ACCEPT)
                .contentType("application/json")
                .header("Mcp-Session-Id", sessionId)
                .body(toolsList)
                .post("/mcp")
                .then()
                .statusCode(200)
                .body(containsString("toUpperSnakeCase"))
                .body(containsString("saveNote"));
    }
}