package com.example.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DevToolkitMcpTest {

    @Test
    void allToolsShouldBeRegistered() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsList(page -> {
                        assertEquals(5, page.size());
                        assertNotNull(page.findByName("toUpperSnakeCase"));
                        assertNotNull(page.findByName("countOccurrences"));
                        assertNotNull(page.findByName("base64Transform"));
                        assertNotNull(page.findByName("truncate"));
                        assertNotNull(page.findByName("saveNote"));
                    })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void toUpperSnakeCaseShouldHandleNormalAndEdgeInput() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsCall("toUpperSnakeCase",
                            java.util.Map.of("input", "my variable name"),
                            r -> {
                                assertFalse(r.isError());
                                assertEquals("MY_VARIABLE_NAME",
                                        r.content().get(0).asText().text());
                            })
                    .toolsCall("toUpperSnakeCase",
                            java.util.Map.of("input", "  spaces everywhere  "),
                            r -> {
                                assertFalse(r.isError());
                                assertEquals("SPACES_EVERYWHERE",
                                        r.content().get(0).asText().text());
                            })
                    .toolsCall("toUpperSnakeCase",
                            java.util.Map.of("input", ""),
                            r -> {
                                assertFalse(r.isError());
                                assertEquals("", r.content().get(0).asText().text());
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void base64TransformShouldSucceedAndFailGracefully() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsCall("base64Transform",
                            java.util.Map.of("input", "hello world", "operation", "encode"),
                            r -> {
                                assertFalse(r.isError());
                                assertTrue(r.content().get(0).asText().text()
                                        .contains("aGVsbG8gd29ybGQ="));
                            })
                    .toolsCall("base64Transform",
                            java.util.Map.of("input", "aGVsbG8gd29ybGQ=", "operation", "decode"),
                            r -> {
                                assertFalse(r.isError());
                                assertTrue(r.content().get(0).asText().text()
                                        .contains("hello world"));
                            })
                    .toolsCall("base64Transform",
                            java.util.Map.of("input", "hello", "operation", "explode"),
                            r -> {
                                assertTrue(r.isError());
                                assertTrue(r.content().get(0).asText().text()
                                        .contains("Unknown operation"));
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void toolSchemasShouldBeCompleteAndDescriptive() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsList(page -> {
                        var truncateTool = page.findByName("truncate");
                        assertNotNull(truncateTool);
                        assertNotNull(truncateTool.description());
                        assertFalse(truncateTool.description().isBlank());
                        assertNotNull(truncateTool.inputSchema());

                        var countTool = page.findByName("countOccurrences");
                        assertNotNull(countTool);
                        assertNotNull(countTool.description());
                        assertFalse(countTool.description().isBlank());
                        assertNotNull(countTool.inputSchema());
                    })
                    .toolsCall("truncate",
                            java.util.Map.of("input", "a".repeat(200), "maxLength", 10),
                            r -> {
                                assertFalse(r.isError());
                                String result = r.content().get(0).asText().text();
                                assertTrue(result.length() <= 10);
                                assertTrue(result.endsWith("..."));
                            })
                    .toolsCall("countOccurrences",
                            java.util.Map.of("text", "banana", "substring", "an"),
                            r -> {
                                assertFalse(r.isError());
                                assertEquals("2", r.content().get(0).asText().text());
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void saveNoteThenReadItBackViaResourceTemplate() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsCall("saveNote",
                            java.util.Map.of("key", "testkey", "content", "# Hello from the test"),
                            r -> {
                                assertFalse(r.isError());
                                assertTrue(r.content().get(0).asText().text()
                                        .contains("testkey"));
                                assertTrue(r.content().get(1).asText().text()
                                        .contains("dev-toolkit://notes/testkey"));
                            })
                    .resourcesRead("dev-toolkit://notes/testkey",
                            r -> {
                                var contents = r.contents().get(0).asText();
                                assertEquals("# Hello from the test", contents.text());
                                assertEquals("dev-toolkit://notes/testkey", contents.uri());
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void saveNoteShouldRejectBlankKey() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsCall("saveNote",
                            java.util.Map.of("key", "", "content", "this should not be saved"),
                            r -> {
                                assertTrue(r.isError());
                                assertTrue(r.content().get(0).asText().text()
                                        .contains("Key must not be blank"));
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void serverInfoShouldListAvailableNoteKeys() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .resourcesList(page -> {
                        assertNotNull(page.findByUri("dev-toolkit://server-info"));
                        assertNotNull(page.findByUri("dev-toolkit://java-string-cheatsheet"));
                    })
                    .resourcesRead("dev-toolkit://server-info",
                            r -> {
                                String content = r.contents().get(0).asText().text();
                                assertTrue(content.contains("Online"));
                                assertTrue(content.contains("conventions"));
                                assertTrue(content.contains("architecture"));
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void readingMissingNoteShouldReturnHelpfulContent() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .resourcesRead("dev-toolkit://notes/doesnotexist",
                            r -> {
                                String content = r.contents().get(0).asText().text();
                                assertTrue(content.contains("Note Not Found"));
                                assertTrue(content.contains("doesnotexist"));
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void resourceTemplateShouldBeRegisteredAndAccessible() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .resourcesTemplatesList(page -> {
                        var template = page.findByUriTemplate("dev-toolkit://notes/{key}");
                        assertNotNull(template);
                        assertEquals("Project Note", template.name());
                    })
                    .resourcesRead("dev-toolkit://notes/conventions",
                            r -> assertTrue(r.contents().get(0).asText().text()
                                    .contains("camelCase")))
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void promptsShouldBeRegisteredWithCorrectArguments() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .promptsList(page -> {
                        assertEquals(2, page.size());

                        var explainError = page.findByName("explain-error");
                        assertNotNull(explainError);
                        assertNotNull(explainError.description());
                        assertTrue(explainError.arguments().stream()
                                .anyMatch(a -> a.name().equals("error")));

                        var codeReview = page.findByName("code-review");
                        assertNotNull(codeReview);
                        assertNotNull(codeReview.description());
                        assertTrue(codeReview.arguments().stream()
                                .anyMatch(a -> a.name().equals("code")));
                    })
                    .promptsGet("explain-error",
                            java.util.Map.of("error", "NullPointerException at line 42"),
                            r -> {
                                assertEquals(1, r.messages().size());
                                String text = r.messages().get(0).content().asText().text();
                                assertTrue(text.contains("NullPointerException"));
                            })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void inspectRawJsonRpcTraffic() {
        var client = McpAssured.newConnectedStreamableClient();
        try {
            client.when()
                    .toolsCall("base64Transform",
                            java.util.Map.of("input", "debug me", "operation", "encode"),
                            r -> {
                            })
                    .thenAssertResults();

            var snapshot = client.snapshot();
            if (!snapshot.requests().isEmpty() && !snapshot.responses().isEmpty()) {
                System.out.println("→ Request:");
                System.out.println(snapshot.requests().get(0).encodePrettily());
                System.out.println("← Response:");
                System.out.println(snapshot.responses().get(0).encodePrettily());
            }
        } finally {
            client.disconnect();
        }
    }

}