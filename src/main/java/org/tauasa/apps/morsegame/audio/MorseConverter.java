package org.tauasa.apps.morsegame.audio;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Encodes plain text → Morse and decodes Morse → plain text.
 *
 * Morse format conventions:
 *   '.'  – dot
 *   '-'  – dash
 *   ' '  – letter separator (single space)
 *   ' / '– word separator
 */
@Component
public class MorseConverter {

    private static final Map<Character, String> ENCODE_MAP = new HashMap<>();
    private static final Map<String, Character> DECODE_MAP = new HashMap<>();

    static {
        String[][] table = {
            // Letters
            {"A",".-"},   {"B","-..."}, {"C","-.-."}, {"D","-.."},
            {"E","."},    {"F","..-."}, {"G","--."},  {"H","...."},
            {"I",".."},   {"J",".---"}, {"K","-.-"},  {"L",".-.."},
            {"M","--"},   {"N","-."},   {"O","---"},  {"P",".--."},
            {"Q","--.-"}, {"R",".-."},  {"S","..."},  {"T","-"},
            {"U","..-"},  {"V","...-"}, {"W",".--"},  {"X","-..-"},
            {"Y","-.--"}, {"Z","--.."},
            // Digits
            {"0","-----"}, {"1",".----"}, {"2","..---"}, {"3","...--"},
            {"4","....-"}, {"5","....."}, {"6","-...."}, {"7","--..."},
            {"8","---.."}, {"9","----."},
            // Punctuation
            {".", ".-.-.-"}, {",", "--..--"}, {"?", "..--.."},
            {"!", "-.-.--"}, {"-", "-....-"}, {"/", "-..-."},
            {"@", ".--.-."}, {"(", "-.--."}, {")", "-.--.-"},
        };
        for (String[] p : table) {
            char ch = p[0].charAt(0);
            String code = p[1];
            ENCODE_MAP.put(ch, code);
            DECODE_MAP.put(code, ch);
        }
    }

    /**
     * Encodes plain text to Morse code.
     * @throws IllegalArgumentException for unsupported characters
     */
    public String encode(String text) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("Input text is empty.");

        StringBuilder sb = new StringBuilder();
        String[] words = text.trim().toUpperCase().split("\\s+");

        for (int w = 0; w < words.length; w++) {
            if (w > 0) sb.append(" / ");
            String word = words[w];
            for (int c = 0; c < word.length(); c++) {
                char ch = word.charAt(c);
                String code = ENCODE_MAP.get(ch);
                if (code == null)
                    throw new IllegalArgumentException(
                        "Unsupported character: '" + ch + "'");
                if (c > 0) sb.append(' ');
                sb.append(code);
            }
        }
        return sb.toString();
    }

    /**
     * Decodes Morse code to plain text.
     * @throws IllegalArgumentException for unknown Morse sequences
     */
    public String decode(String morse) {
        if (morse == null || morse.isBlank())
            throw new IllegalArgumentException("Morse input is empty.");

        String normalised = morse.trim().replaceAll(" {2,}", " ");
        StringBuilder sb = new StringBuilder();
        String[] words = normalised.split(" / ");

        for (int w = 0; w < words.length; w++) {
            if (w > 0) sb.append(' ');
            for (String code : words[w].trim().split(" ")) {
                if (code.isEmpty()) continue;
                Character ch = DECODE_MAP.get(code);
                if (ch == null)
                    throw new IllegalArgumentException(
                        "Unknown Morse sequence: '" + code + "'");
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
