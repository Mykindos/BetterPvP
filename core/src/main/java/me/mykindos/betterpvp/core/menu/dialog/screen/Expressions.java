package me.mykindos.betterpvp.core.menu.dialog.screen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Evaluates the {@code {...}} parts of screen text and conditions against a state lookup.
 * <p>
 * Grammar: literals ({@code 12}, {@code 'text'}, {@code true}, {@code null}), paths ({@code tab}, {@code skill.name},
 * {@code list[0]}), arithmetic ({@code + - * /}, where {@code +} joins text), comparisons ({@code == != < <= > >=}),
 * {@code && || !}, parentheses, and formatters applied with a pipe ({@code coins | short},
 * {@code name | default:'Nobody'}). Paths read maps, lists, and getters or record-style
 * accessors on Java objects.
 */
public final class Expressions {

    private Expressions() {
    }

    /** Evaluates a template. A template that is exactly one {@code {expr}} keeps the value's type. */
    public static Object evaluate(String template, Function<String, Object> lookup,
                                  Map<String, BiFunction<Object, List<Object>, Object>> formatters) {
        final String trimmed = template.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.indexOf('{', 1) < 0) {
            return new Parser(trimmed.substring(1, trimmed.length() - 1), lookup, formatters).parse();
        }
        final StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < template.length()) {
            final int open = template.indexOf('{', index);
            if (open < 0) {
                out.append(template, index, template.length());
                break;
            }
            final int close = template.indexOf('}', open);
            if (close < 0) {
                throw new IllegalArgumentException("Unclosed { in " + template);
            }
            out.append(template, index, open);
            out.append(text(new Parser(template.substring(open + 1, close), lookup, formatters).parse()));
            index = close + 1;
        }
        return out.toString();
    }

    public static boolean truthy(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean bool -> bool;
            case Number number -> number.doubleValue() != 0;
            case String string -> !string.isEmpty();
            case Collection<?> collection -> !collection.isEmpty();
            default -> true;
        };
    }

    public static String text(Object value) {
        if (value instanceof Double number && number == Math.rint(number)) {
            return String.valueOf(number.longValue());
        }
        return String.valueOf(value);
    }

    /** Reads one step of a path from a map, list or Java object, or null when it has none. */
    static Object member(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        if (owner instanceof Map<?, ?> map) {
            return map.get(name);
        }
        final String capital = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (String candidate : List.of(name, "get" + capital, "is" + capital)) {
            try {
                final Method method = owner.getClass().getMethod(candidate);
                return method.invoke(owner);
            } catch (ReflectiveOperationException ignored) {
                // Try the next accessor style.
            }
        }
        return null;
    }

    private static final class Parser {
        private final String source;
        private final Function<String, Object> lookup;
        private final Map<String, BiFunction<Object, List<Object>, Object>> formatters;
        private int position;

        private Parser(String source, Function<String, Object> lookup,
                       Map<String, BiFunction<Object, List<Object>, Object>> formatters) {
            this.source = source;
            this.lookup = lookup;
            this.formatters = formatters;
        }

        Object parse() {
            final Object value = or();
            skipSpace();
            if (position != source.length()) {
                throw error("Unexpected '" + source.substring(position) + "'");
            }
            return value;
        }

        private Object or() {
            Object left = and();
            while (take("||")) {
                final Object right = and();
                left = truthy(left) || truthy(right);
            }
            return left;
        }

        private Object and() {
            Object left = comparison();
            while (take("&&")) {
                final Object right = comparison();
                left = truthy(left) && truthy(right);
            }
            return left;
        }

        private Object comparison() {
            final Object left = additive();
            for (String operator : List.of("==", "!=", "<=", ">=", "<", ">")) {
                if (take(operator)) {
                    return compare(operator, left, additive());
                }
            }
            return left;
        }

        private Object additive() {
            Object left = multiplicative();
            while (true) {
                if (take("+")) {
                    final Object right = multiplicative();
                    left = left instanceof Number a && right instanceof Number b
                            ? (Object) (a.doubleValue() + b.doubleValue())
                            : text(left) + text(right);
                } else if (take("-")) {
                    left = number(left) - number(multiplicative());
                } else {
                    return left;
                }
            }
        }

        private Object multiplicative() {
            Object left = unary();
            while (true) {
                if (take("*")) {
                    left = number(left) * number(unary());
                } else if (take("/")) {
                    left = number(left) / number(unary());
                } else {
                    return left;
                }
            }
        }

        private double number(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            throw error("Expected a number, got " + value);
        }

        private Object unary() {
            if (take("!")) {
                return !truthy(unary());
            }
            Object value = primary();
            while (take("|")) {
                final String name = identifier();
                final List<Object> args = new ArrayList<>();
                while (take(":")) {
                    args.add(primary());
                }
                final BiFunction<Object, List<Object>, Object> formatter = formatters.get(name);
                if (formatter == null) {
                    throw error("Unknown formatter '" + name + "'");
                }
                value = formatter.apply(value, args);
            }
            return value;
        }

        private Object primary() {
            skipSpace();
            if (take("(")) {
                final Object value = or();
                expect(")");
                return value;
            }
            if (position < source.length() && source.charAt(position) == '\'') {
                final int end = source.indexOf('\'', position + 1);
                if (end < 0) {
                    throw error("Unclosed string");
                }
                final String value = source.substring(position + 1, end);
                position = end + 1;
                return value;
            }
            if (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '-')) {
                final int start = position;
                position++;
                while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                    position++;
                }
                return Double.parseDouble(source.substring(start, position));
            }
            final String name = identifier();
            switch (name) {
                case "true" -> {
                    return true;
                }
                case "false" -> {
                    return false;
                }
                case "null" -> {
                    return null;
                }
                default -> {
                }
            }
            Object value = lookup.apply(name);
            while (true) {
                if (take(".")) {
                    value = member(value, identifier());
                } else if (take("[")) {
                    final Object index = or();
                    expect("]");
                    value = value instanceof List<?> list && index instanceof Number number
                            && number.intValue() >= 0 && number.intValue() < list.size() ? list.get(number.intValue()) : null;
                } else {
                    return value;
                }
            }
        }

        private String identifier() {
            skipSpace();
            final int start = position;
            while (position < source.length()
                    && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) {
                position++;
            }
            if (start == position) {
                throw error("Expected a name");
            }
            return source.substring(start, position);
        }

        private boolean take(String token) {
            skipSpace();
            if (source.startsWith(token, position)) {
                // "|" must not swallow the first half of "||", and "<"/">" must not swallow "<="/">=".
                if (token.equals("|") && source.startsWith("||", position)) {
                    return false;
                }
                position += token.length();
                return true;
            }
            return false;
        }

        private void expect(String token) {
            if (!take(token)) {
                throw error("Expected '" + token + "'");
            }
        }

        private void skipSpace() {
            while (position < source.length() && source.charAt(position) == ' ') {
                position++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " in {" + source + "}");
        }

        private static Object compare(String operator, Object left, Object right) {
            if (left instanceof Number a && right instanceof Number b) {
                final double x = a.doubleValue();
                final double y = b.doubleValue();
                return switch (operator) {
                    case "==" -> x == y;
                    case "!=" -> x != y;
                    case "<" -> x < y;
                    case "<=" -> x <= y;
                    case ">" -> x > y;
                    default -> x >= y;
                };
            }
            final boolean equal = left == null ? right == null : right != null && text(left).equals(text(right));
            return switch (operator) {
                case "==" -> equal;
                case "!=" -> !equal;
                default -> throw new IllegalArgumentException("Cannot compare " + left + " " + operator + " " + right);
            };
        }
    }
}
