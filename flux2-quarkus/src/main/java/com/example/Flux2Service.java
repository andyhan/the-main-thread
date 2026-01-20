package com.example;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Flux2Service {

    @ConfigProperty(name = "flux2.model.path")
    String modelPath;

    @ConfigProperty(name = "flux2.output.dir", defaultValue = "target/flux2-output")
    String outputDir;

    public String getOutputDir() {
        return outputDir;
    }

    private MemorySegment context;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Path.of(outputDir));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create output directory", e);
        }

        context = Flux2Native.initialize(modelPath);

        if (context == null || context.address() == 0) {
            throw new IllegalStateException("Flux2 returned a null context");
        }
    }

    @PreDestroy
    void shutdown() {
        if (context != null && context.address() != 0) {
            Flux2Native.free(context);
        }
    }

    public GenerationResult generate(String prompt, int width, int height, int steps) {
        String filename = UUID.randomUUID() + ".png";
        String path = Path.of(outputDir, filename).toString();

        int result = Flux2Native.generate(context, prompt, path, width, height, steps);

        if (result != 0) {
            throw new IllegalStateException("Flux2 failed with error code " + result);
        }

        return new GenerationResult(filename, path);
    }

    public record GenerationResult(String filename, String fullPath) {
    }
}