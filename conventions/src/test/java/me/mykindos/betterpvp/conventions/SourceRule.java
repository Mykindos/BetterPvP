package me.mykindos.betterpvp.conventions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * House rules that only show in the source text, not in compiled classes. Each finds the lines that break it.
 */
enum SourceRule {

    FULLY_QUALIFIED_NAME("fully qualified class name in code, import it instead") {
        private final Pattern pattern = Pattern.compile(
                "(?<![\\w.])(?:java|javax|org|net|com|me|io|lombok|kotlin)\\.(?:[a-z_][a-z0-9_]*\\.)+[A-Z]\\w*");

        @Override
        List<Integer> find(JavaText text) {
            List<Integer> lines = new ArrayList<>();
            Matcher matcher = pattern.matcher(text.getCode());
            while (matcher.find()) {
                if (!isImportOrPackage(text.getCode(), matcher.start())) {
                    lines.add(text.lineOf(matcher.start()));
                }
            }
            return lines;
        }
    },

    BANNER_COMMENT("banner or divider comment") {
        private final Pattern pattern = Pattern.compile("//\\s*[─━═=\\-~*#]{3,}");

        @Override
        List<Integer> find(JavaText text) {
            return lines(pattern, text.getComments(), text);
        }
    },

    INLINE_ARRAY_LOOP("loop over a throwaway array, unroll it or take discrete parameters") {
        private final Pattern pattern = Pattern.compile("\\bfor\\s*\\([^:;)]+:\\s*new\\s+\\w+\\s*\\[\\s*]\\s*\\{");

        @Override
        List<Integer> find(JavaText text) {
            return lines(pattern, text.getCode(), text);
        }
    },

    HARDCODED_PLAYER_TEXT("hardcoded player-facing text, use Translations.component") {
        private final Pattern pattern = Pattern.compile(
                "\\bUtilMessage\\.(?:simpleMessage|message)\\(\\s*\\w+\\s*,\\s*\"[^\"]*\"\\s*,\\s*\"[A-Za-z<]");

        @Override
        List<Integer> find(JavaText text) {
            List<Integer> lines = new ArrayList<>();
            Matcher matcher = pattern.matcher(text.getSource());
            while (matcher.find()) {
                if (text.inCode(matcher.start())) {
                    lines.add(text.lineOf(matcher.start()));
                }
            }
            return lines;
        }
    },

    REFACTOR_NARRATIVE("comment narrates history, describe the end state") {
        private final Pattern pattern = Pattern.compile(
                "\\b(?:previously|used to be|formerly|was replaced by|replaces the (?:old|previous))\\b",
                Pattern.CASE_INSENSITIVE);

        @Override
        List<Integer> find(JavaText text) {
            return lines(pattern, text.getComments(), text);
        }
    },

    UNSUBMITTED_LOG("log call without .submit(), nothing is emitted") {
        private final Pattern pattern = Pattern.compile("\\blog\\.(?:info|warn|error|debug|trace)\\s*\\(");

        @Override
        List<Integer> find(JavaText text) {
            List<Integer> lines = new ArrayList<>();
            if (!text.getCode().contains("@CustomLog")) {
                return lines;
            }
            String code = text.getCode();
            Matcher matcher = pattern.matcher(code);
            while (matcher.find()) {
                int end = statementEnd(code, matcher.end() - 1);
                if (!code.substring(matcher.start(), end).replaceAll("\\s", "").contains(".submit()")) {
                    lines.add(text.lineOf(matcher.start()));
                }
            }
            return lines;
        }
    };

    private final String description;

    SourceRule(String description) {
        this.description = description;
    }

    String description() {
        return description;
    }

    abstract List<Integer> find(JavaText text);

    /**
     * The breaking lines, minus those that carry a {@code // conventions:allow RULE_NAME} comment for a genuine
     * exception, such as two imported types sharing a simple name.
     */
    List<Integer> violations(JavaText text) {
        String[] commentLines = text.getComments().split("\n", -1);
        String allow = "conventions:allow " + name();
        return find(text).stream()
                .filter(line -> !commentLines[line - 1].contains(allow))
                .toList();
    }

    private static List<Integer> lines(Pattern pattern, String view, JavaText text) {
        List<Integer> lines = new ArrayList<>();
        Matcher matcher = pattern.matcher(view);
        while (matcher.find()) {
            lines.add(text.lineOf(matcher.start()));
        }
        return lines;
    }

    private static boolean isImportOrPackage(String code, int offset) {
        int lineStart = code.lastIndexOf('\n', offset) + 1;
        String line = code.substring(lineStart, offset).stripLeading();
        return line.startsWith("import ") || line.startsWith("package ");
    }

    /**
     * The offset just past the semicolon that ends the statement whose first parenthesis is at {@code open}.
     */
    private static int statementEnd(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '(' || c == '{') {
                depth++;
            } else if (c == ')' || c == '}') {
                depth--;
            } else if (c == ';' && depth <= 0) {
                return i + 1;
            }
        }
        return code.length();
    }
}
