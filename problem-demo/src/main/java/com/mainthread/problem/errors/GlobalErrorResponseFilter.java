package com.mainthread.problem.errors;

import java.util.Collections;
import java.util.HashMap;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

public class GlobalErrorResponseFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Ensure the Problem schema is defined (the library may add it, but we ensure it exists)
        ensureProblemSchema(openAPI);

        // Add the 400 and 500 responses to all operations
        openAPI.getPaths().getPathItems().values()
                .forEach(pathItem -> pathItem.getOperations().values().forEach(operation -> {

                    if (!operation.getResponses().hasAPIResponse("400")) {
                        operation.getResponses()
                                .addAPIResponse("400", createProblemResponse("Bad Request"));
                    }

                    if (!operation.getResponses().hasAPIResponse("500")) {
                        operation.getResponses()
                                .addAPIResponse("500", createProblemResponse("Internal Server Error"));
                    }
                }));
    }

    private void ensureProblemSchema(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        // Get existing schemas (may be null or unmodifiable)
        var existingSchemas = openAPI.getComponents().getSchemas();

        // Only add the schema if it doesn't already exist
        if (existingSchemas == null || !existingSchemas.containsKey("Problem")) {
            Schema typeSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                    .format("uri")
                    .description("A URI reference that identifies the problem type");

            Schema titleSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                    .description("A short, human-readable summary of the problem type");

            Schema statusSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.INTEGER))
                    .format("int32")
                    .description("The HTTP status code");

            Schema detailSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                    .description("A human-readable explanation specific to this occurrence of the problem");

            Schema instanceSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                    .format("uri")
                    .description("A URI reference that identifies the specific occurrence of the problem");

            Schema problemSchema = OASFactory.createSchema()
                    .type(Collections.singletonList(Schema.SchemaType.OBJECT))
                    .addProperty("type", typeSchema)
                    .addProperty("title", titleSchema)
                    .addProperty("status", statusSchema)
                    .addProperty("detail", detailSchema)
                    .addProperty("instance", instanceSchema)
                    .description("RFC 7807 Problem Details for HTTP APIs (compatible with RFC 9457)");

            // Create a new modifiable map with existing schemas and add the Problem schema
            var schemas = new HashMap<String, Schema>();
            if (existingSchemas != null) {
                schemas.putAll(existingSchemas);
            }
            schemas.put("Problem", problemSchema);
            openAPI.getComponents().setSchemas(schemas);
        }
    }

    private APIResponse createProblemResponse(String description) {
        // Reference the Problem schema
        Schema problemSchema = OASFactory.createSchema()
                .ref("#/components/schemas/Problem");

        return OASFactory.createAPIResponse()
                .description(description)
                .content(
                        OASFactory.createContent().addMediaType(
                                "application/problem+json",
                                OASFactory.createMediaType().schema(problemSchema)));
    }
}