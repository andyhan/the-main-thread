package com.example.mcp;

import java.util.List;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class NoteTools {

    @Inject
    ProjectResources projectResources;

    @Tool(description = "Save or update a project note by key. This has side effects.")
    ToolResponse saveNote(
            @ToolArg(description = "Note key, e.g. 'conventions'") String key,
            @ToolArg(description = "Markdown content") String content) {

        if (key == null || key.isBlank()) {
            return ToolResponse.error("Key must not be blank.");
        }
        if (content == null) {
            content = "";
        }

        projectResources.putNote(key, content);

        return ToolResponse.success(List.of(
                new TextContent("Saved note: " + key),
                new TextContent("Read it via: dev-toolkit://notes/" + key)));
    }
}