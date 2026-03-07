package dev.example;

import java.nio.file.Files;
import java.nio.file.Path;

import io.brunoborges.jairosvg.JairoSVG;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SvgConversionService {

    public byte[] convertToPng(byte[] svgBytes, double scale, int dpi) throws Exception {
        return JairoSVG.builder()
                .fromBytes(svgBytes)
                .scale(scale)
                .dpi(dpi)
                .toPng();
    }

    public byte[] convertToPng(Path svgPath, double scale, int dpi) throws Exception {
        byte[] svgBytes = Files.readAllBytes(svgPath);
        return convertToPng(svgBytes, scale, dpi);
    }

}