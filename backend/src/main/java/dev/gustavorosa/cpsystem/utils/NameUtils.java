package dev.gustavorosa.cpsystem.utils;

import java.util.Locale;
import java.util.Set;

public class NameUtils {

    private static final Set<String> CONNECTIVES = Set.of("de", "da", "do", "das", "dos", "e");

    /**
     * Normalizes a person name: trims, collapses whitespace and applies Title Case,
     * keeping Portuguese connectives (de, da, do, das, dos, e) in lowercase unless
     * they are the first word. Idempotent.
     */
    public static String toTitleCase(String input) {
        if (input == null) {
            return null;
        }
        if (input.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] words = input.trim().toLowerCase(Locale.ROOT).split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i > 0) {
                result.append(" ");
            }
            if (i > 0 && CONNECTIVES.contains(word)) {
                result.append(word);
            } else {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }
}
