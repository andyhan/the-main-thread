package com.example;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.forest.flux.FluxLib;

public class Flux2Native {

    @ConfigProperty(name = "flux2.model.path")
    String modelsDir;

    public static MemorySegment initialize(String modelPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment modelPathNative = arena.allocateFrom(modelPath);
            return FluxLib.flux_wrapper_init(modelPathNative);
        }
    }

    public static int generate(MemorySegment context, String prompt, String path, int width, int height, int steps) {
        float guidance = 3.5f;
        long seed = System.currentTimeMillis();
        System.out.printf("Params: %dx%d, steps=%d, guidance=%.2f, seed=%d%n", width, height, steps, guidance, seed);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment promptNative = arena.allocateFrom(prompt);
            MemorySegment outputPathNative = arena.allocateFrom(path);

            return FluxLib.flux_wrapper_generate(context, promptNative, outputPathNative, width, height, steps,
                    guidance, seed);
        }
    }

    public static void free(MemorySegment context) {
        FluxLib.flux_wrapper_free(context);
    }
}