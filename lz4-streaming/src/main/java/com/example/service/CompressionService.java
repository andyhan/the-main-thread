package com.example.service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

@ApplicationScoped
public class CompressionService {

    private final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();

    private final AtomicLong rawBytes = new AtomicLong();
    private final AtomicLong compressedBytes = new AtomicLong();
    private final AtomicLong operations = new AtomicLong();

    public byte[] compressWithHeader(String data) {
        byte[] input = data.getBytes(StandardCharsets.UTF_8);
        rawBytes.addAndGet(input.length);

        byte[] compressed = new byte[compressor.maxCompressedLength(input.length)];
        int size = compressor.compress(input, 0, input.length, compressed, 0);

        byte[] result = new byte[size + 4];
        result[0] = (byte) (input.length >>> 24);
        result[1] = (byte) (input.length >>> 16);
        result[2] = (byte) (input.length >>> 8);
        result[3] = (byte) input.length;

        System.arraycopy(compressed, 0, result, 4, size);

        compressedBytes.addAndGet(result.length);
        operations.incrementAndGet();

        return result;
    }

    public double ratio() {
        long raw = rawBytes.get();
        return raw == 0 ? 1.0 : (double) raw / compressedBytes.get();
    }

    public void reset() {
        rawBytes.set(0);
        compressedBytes.set(0);
        operations.set(0);
    }

    public long rawBytes() {
        return rawBytes.get();
    }

    public long compressedBytes() {
        return compressedBytes.get();
    }

    public long operations() {
        return operations.get();
    }
}