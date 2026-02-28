package org.example.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Supplies the OIDC public key from classpath:publicKey.pem so application.properties
 * does not need to inline the key. Generate the keypair with openssl (see article.md).
 */
public class PublicKeyConfigSource implements ConfigSource {

    private static final String PUBLIC_KEY_PROPERTY = "quarkus.oidc.public-key";
    private static final String PUBLIC_KEY_RESOURCE = "publicKey.pem";

    @Override
    public Set<String> getPropertyNames() {
        return Set.of(PUBLIC_KEY_PROPERTY);
    }

    @Override
    public String getValue(String propertyName) {
        if (!PUBLIC_KEY_PROPERTY.equals(propertyName)) {
            return null;
        }
        try {
            return loadPublicKeyFromClasspath();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + PUBLIC_KEY_RESOURCE + " from classpath", e);
        }
    }

    @Override
    public String getName() {
        return "PublicKeyClasspathConfigSource";
    }

    @Override
    public int getOrdinal() {
        return 260;
    }

    private static String loadPublicKeyFromClasspath() throws IOException {
        try (Reader reader = new InputStreamReader(
                PublicKeyConfigSource.class.getClassLoader().getResourceAsStream(PUBLIC_KEY_RESOURCE),
                StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[512];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString().trim();
        }
    }
}
