package com.example.mcp;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.ResourceTemplateArg;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NoteTemplates {

    @Inject
    ProjectResources projectResources;

    @ResourceTemplate(uriTemplate = "dev-toolkit://notes/{key}", name = "Project Note", description = "Retrieve a project note by key")
    TextResourceContents getNote(
            @ResourceTemplateArg(name = "key") String key,
            RequestUri uri) {

        String note = projectResources.getNotes().get(key);
        if (note == null) {
            String content = """
                    # Note Not Found

                    No note exists with key: `%s`

                    Available keys: %s
                    """.formatted(key, String.join(", ", projectResources.getNotes().keySet()));

            return new TextResourceContents(uri.value(), content, "text/markdown");
        }

        return new TextResourceContents(uri.value(), note, "text/markdown");
    }
}