package com.secure.text.util;

import java.lang.Character.UnicodeBlock;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TextSanitizer {

    // THREAT 1: Invisible Characters
    // Removes ZWSP, ZWNJ, ZWJ, BOM, and Soft Hyphen
    private static final Pattern INVISIBLE_CHARS = Pattern.compile(
            "[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]");

    // THREAT 2: BiDi Overrides
    // Removes Left-to-Right and Right-to-Left overrides that mask extensions
    private static final Pattern BIDI_OVERRIDE = Pattern.compile(
            "[\\u202A-\\u202E\\u2066-\\u2069]");

    // THREAT 3: Control Characters
    // Removes ASCII control chars (Bell, Escape, etc.) but KEEPS \r\n\t
    private static final Pattern CONTROL_CHARS = Pattern.compile(
            "[\\p{Cc}&&[^\r\n\t]]");

    /**
     * The Master Cleaning Function.
     * Call this BEFORE saving data to your database.
     */
    public static String sanitize(String input) {
        if (input == null)
            return null;

        // 1. Normalize: Fix the "café" problem (Precomposed vs Decomposed)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);

        // 2. Scrub: Remove invisible/dangerous characters
        normalized = INVISIBLE_CHARS.matcher(normalized).replaceAll("");
        normalized = BIDI_OVERRIDE.matcher(normalized).replaceAll("");
        normalized = CONTROL_CHARS.matcher(normalized).replaceAll("");

        return normalized.trim();
    }

    /**
     * The Validation Function.
     * Call this inside your @SafeText validator.
     */
    public static boolean isSafe(String input) {
        if (input == null)
            return true;

        // If we find any match, the string is unsafe
        return !INVISIBLE_CHARS.matcher(input).find() &&
                !BIDI_OVERRIDE.matcher(input).find() &&
                !CONTROL_CHARS.matcher(input).find();
    }

    /**
     * THREAT 3: Homograph Detection
     * Checks if input dangerously mixes scripts (e.g. Cyrillic + Latin).
     * Call this explicitly in your Controller for sensitive fields like usernames.
     */
    public static boolean isSuspiciousMixedScript(String input) {
        if (input == null || input.isEmpty())
            return false;

        Set<UnicodeBlock> blocks = new HashSet<>();

        // Analyze every code point (character) in the string
        input.codePoints().forEach(cp -> {
            UnicodeBlock block = UnicodeBlock.of(cp);
            if (block != null && !isCommonBlock(block)) {
                blocks.add(block);
            }
        });

        // The Rule: You cannot have Cyrillic AND Latin in the same string.
        boolean hasCyrillic = blocks.contains(UnicodeBlock.CYRILLIC);
        boolean hasLatin = blocks.contains(UnicodeBlock.LATIN_EXTENDED_A) ||
                blocks.contains(UnicodeBlock.BASIC_LATIN);

        return hasCyrillic && hasLatin;
    }

    // Helper to ignore "safe" shared characters like spaces, numbers, and
    // punctuation
    private static boolean isCommonBlock(UnicodeBlock block) {
        return block == UnicodeBlock.COMMON_INDIC_NUMBER_FORMS ||
                block == UnicodeBlock.LATIN_1_SUPPLEMENT;
        // Note: BASIC_LATIN is NOT filtered out because we need to detect
        // homograph attacks where Cyrillic and Latin are mixed
    }
}