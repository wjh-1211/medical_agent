package com.medicalagent.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class LexicalTokenizer {

    List<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        List<String> terms = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        int[] codePoints = normalized.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            int codePoint = codePoints[index];
            if (Character.isLetterOrDigit(codePoint) && codePoint < 128) {
                latin.appendCodePoint(codePoint);
                continue;
            }
            flushLatin(terms, latin);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                terms.add(new String(Character.toChars(codePoint)));
                if (index + 1 < codePoints.length && Character.UnicodeScript.of(codePoints[index + 1]) == Character.UnicodeScript.HAN) {
                    terms.add(new String(new int[]{codePoint, codePoints[index + 1]}, 0, 2));
                }
            }
        }
        flushLatin(terms, latin);
        return terms;
    }

    Set<String> uniqueTokens(String text) {
        return new LinkedHashSet<>(tokenize(text));
    }

    private void flushLatin(List<String> terms, StringBuilder latin) {
        if (latin.length() > 0) {
            terms.add(latin.toString());
            latin.setLength(0);
        }
    }
}
