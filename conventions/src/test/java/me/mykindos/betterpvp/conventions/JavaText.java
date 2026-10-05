package me.mykindos.betterpvp.conventions;

import lombok.Getter;

/**
 * A Java source split into two views of the same length: {@link #getCode()} keeps code with string, char and text
 * block contents and all comments blanked, and {@link #getComments()} keeps only comments. Newlines survive in both,
 * so an offset maps to the same line in the original, the code view and the comment view.
 */
@Getter
final class JavaText {

    private final String source;
    private final String code;
    private final String comments;

    JavaText(String source) {
        this.source = source;
        StringBuilder code = new StringBuilder(source.length());
        StringBuilder comments = new StringBuilder(source.length());
        int i = 0;
        int length = source.length();
        while (i < length) {
            char c = source.charAt(i);
            if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                end = end < 0 ? length : end;
                appendSplit(source, i, end, comments, code);
                i = end;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                end = end < 0 ? length : end + 2;
                appendSplit(source, i, end, comments, code);
                i = end;
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                end = end < 0 ? length : end + 3;
                appendLiteral(source, i, end, 3, code, comments);
                i = end;
            } else if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < length && source.charAt(end) != c && source.charAt(end) != '\n') {
                    end += source.charAt(end) == '\\' ? 2 : 1;
                }
                end = Math.min(end + 1, length);
                appendLiteral(source, i, end, 1, code, comments);
                i = end;
            } else {
                code.append(c);
                comments.append(blank(c));
                i++;
            }
        }
        this.code = code.toString();
        this.comments = comments.toString();
    }

    int lineOf(int offset) {
        return (int) source.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
    }

    /**
     * Whether the non-whitespace character at this offset is code, not part of a comment or a literal's contents.
     */
    boolean inCode(int offset) {
        return code.charAt(offset) == source.charAt(offset);
    }

    private static void appendSplit(String source, int from, int to, StringBuilder kept, StringBuilder blanked) {
        for (int i = from; i < to; i++) {
            char c = source.charAt(i);
            kept.append(c);
            blanked.append(blank(c));
        }
    }

    private static void appendLiteral(String source, int from, int to, int quote, StringBuilder code,
                                      StringBuilder comments) {
        for (int i = from; i < to; i++) {
            char c = source.charAt(i);
            boolean delimiter = i < from + quote || i >= to - quote;
            code.append(delimiter ? c : blank(c));
            comments.append(blank(c));
        }
    }

    private static char blank(char c) {
        return c == '\n' || c == '\r' ? c : ' ';
    }
}
