package dev.example;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SvgConverterResourceTest {

    static final String SAMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="blue"/>
            </svg>
            """;

    @Test
    void testSvgToPngConversion() throws Exception {

        Path tempSvg = Files.createTempFile("test", ".svg");
        Files.writeString(tempSvg, SAMPLE_SVG);

        byte[] pngBytes = given()
                .multiPart("file", tempSvg.toFile(), "image/svg+xml")
                .multiPart("scale", "1.0")
                .multiPart("dpi", "96")
                .when()
                .post("/convert/png")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .extract()
                .asByteArray();

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);
    }
}