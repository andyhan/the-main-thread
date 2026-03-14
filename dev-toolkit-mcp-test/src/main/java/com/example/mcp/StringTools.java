package com.example.mcp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class StringTools {

    @Tool(description = "Convert a string to UPPER_SNAKE_CASE, useful for generating constant names.")
    String toUpperSnakeCase(
            @ToolArg(description = "The input string, e.g. 'my variable name'") String input) {

        if (input == null) {
            return "";
        }

        return input.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_]", "")
                .toUpperCase();
    }

    @Tool(description = "Count occurrences of a substring within a larger string.")
    String countOccurrences(
            @ToolArg(description = "The text to search in") String text,
            @ToolArg(description = "The substring to count") String substring) {

        if (text == null || text.isEmpty() || substring == null || substring.isEmpty()) {
            return "0";
        }

        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return String.valueOf(count);
    }

    @Tool(description = "Encode or decode a string using Base64. Returns both the result and metadata.")
    ToolResponse base64Transform(
            @ToolArg(description = "The string to process") String input,
            @ToolArg(description = "Operation: 'encode' or 'decode'") String operation) {

        if (input == null) {
            input = "";
        }
        if (operation == null) {
            operation = "";
        }

        try {
            String result;
            if ("encode".equalsIgnoreCase(operation)) {
                result = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
            } else if ("decode".equalsIgnoreCase(operation)) {
                result = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
            } else {
                return ToolResponse.error("Unknown operation: '" + operation + "'. Use 'encode' or 'decode'.");
            }

            return ToolResponse.success(List.of(
                    new TextContent("Result: " + result),
                    new TextContent("Operation: " + operation + " | Input length: " + input.length())));
        } catch (Exception e) {
            return ToolResponse.error("Error: " + e.getMessage());
        }
    }

    @Tool(description = "Truncate a string to a maximum length, appending an ellipsis if truncated.")
    String truncate(
            @ToolArg(description = "The string to truncate") String input,
            @ToolArg(description = "Maximum character length (default: 100)") Integer maxLength) {

        if (input == null) {
            return "";
        }

        int limit = (maxLength != null && maxLength > 0) ? maxLength : 100;
        if (input.length() <= limit) {
            return input;
        }
        if (limit < 4) {
            return input.substring(0, limit);
        }
        return input.substring(0, limit - 3) + "...";
    }
}