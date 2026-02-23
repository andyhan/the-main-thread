package com.example.mcp;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkiverse.mcp.server.BlobResourceContents;
import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Singleton;

@Singleton
public class ProjectResources {

    private final Map<String, String> notes = new ConcurrentHashMap<>();

    public ProjectResources() {
        notes.put("conventions",
                """
                        # Code Conventions

                        - Use camelCase for methods
                        - Use PascalCase for classes
                        - Max line length: 120 chars
                        - Prefer records over POJOs
                        """);

        notes.put("architecture",
                """
                        # Architecture Notes

                        - Hexagonal architecture
                        - Domain layer has zero dependencies
                        - Adapters live in `infrastructure` package
                        """);
    }

    @Resource(uri = "dev-toolkit://server-info", name = "Server Info", description = "Current server status and timestamp")
    TextResourceContents serverInfo(RequestUri uri) {
        String content = """
                # Dev Toolkit MCP Server

                Status: Online
                Time: %s

                ## Available Note Keys
                %s
                """.formatted(
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                String.join(", ", notes.keySet()));

        return new TextResourceContents(uri.value(), content, "text/markdown");
    }

    @Resource(uri = "dev-toolkit://java-string-cheatsheet", name = "Java String Cheat Sheet", description = "Quick reference for common Java String methods")
    TextResourceContents javaStringCheatsheet(RequestUri uri) {
        String content = """
                # Java String Cheat Sheet

                ## Basic Operations
                - `s.length()`
                - `s.isEmpty()`
                - `s.isBlank()`
                - `s.trim()` and `s.strip()`

                ## Searching
                - `s.contains(sub)`
                - `s.indexOf(sub)`
                - `s.startsWith(prefix)` and `s.endsWith(suffix)`

                ## Transforming
                - `s.toUpperCase()` and `s.toLowerCase()`
                - `s.replace(old, new)` and `s.replaceAll(regex, replacement)`
                - `s.substring(start, end)`
                - `String.join(delimiter, parts...)`
                """;
        return new TextResourceContents(uri.value(), content, "text/markdown");
    }

    @Resource(uri = "dev-toolkit://sample-icon", name = "Sample Icon", description = "A tiny sample binary resource (1x1 red pixel PNG)")
    BlobResourceContents sampleIcon(RequestUri uri) {
        byte[] tinyPng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
                0x00, 0x02, 0x00, 0x01, (byte) 0xE2, 0x21, (byte) 0xBC, 0x33,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };

        return new BlobResourceContents(uri.value(), Base64.getEncoder().encodeToString(tinyPng), "image/png");
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void putNote(String key, String content) {
        notes.put(key, content);
    }
}