package academy.themainthread.badge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RecipientIdentity {

    private RecipientIdentity() {}

    public static String sha256Hex(String email, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = email + salt;
            byte[] hash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String openBadgeIdentity(String email, String salt) {
        return "sha256$" + sha256Hex(email, salt);
    }
}