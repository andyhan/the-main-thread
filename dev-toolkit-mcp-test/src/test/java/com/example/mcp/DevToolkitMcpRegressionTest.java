package com.example.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DevToolkitMcpRegressionTest {

    @Test
    void fullContractSmokePass() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsList(page -> {
                        assertEquals(5, page.size());
                        assertNotNull(page.findByName("toUpperSnakeCase"));
                        assertNotNull(page.findByName("saveNote"));
                    })
                    .resourcesList(page -> {
                        assertNotNull(page.findByUri("dev-toolkit://server-info"));
                    })
                    .resourcesTemplatesList(page -> {
                        assertNotNull(page.findByUriTemplate("dev-toolkit://notes/{key}"));
                    })
                    .promptsList(page -> {
                        assertEquals(2, page.size());
                        assertNotNull(page.findByName("code-review"));
                    })
                    .toolsCall("toUpperSnakeCase",
                            Map.of("input", "hello world"),
                            r -> assertEquals("HELLO_WORLD",
                                    r.content().get(0).asText().text()))
                    .toolsCall("countOccurrences",
                            Map.of("text", "abcabc", "substring", "abc"),
                            r -> assertEquals("2",
                                    r.content().get(0).asText().text()))
                    .toolsCall("saveNote",
                            Map.of("key", "smoketest", "content", "# Smoke"),
                            r -> assertFalse(r.isError()))
                    .thenAssertResults();
            client.when()
                    .resourcesRead("dev-toolkit://notes/smoketest",
                            r -> assertTrue(r.contents().get(0).asText().text()
                                    .contains("# Smoke")))
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }
}