package me.mykindos.betterpvp.core.loot.expression;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.loot.LootContext;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Sandboxed expression evaluator backed by Apache Commons JEXL.
 * <p>
 * Expressions are restricted: no scripts, no loops, no {@code new}, no method calls on objects,
 * no assignments. The default namespace exposes {@link LootFunctions}. Variables come from a
 * {@link LootContext}'s inputs merged with optional per-call extras. Reserved names
 * (e.g. {@code roll_index}, {@code bundle_size}, {@code history_size}, {@code source}) are
 * supplied by the loot system at evaluation time.
 */
@CustomLog
public final class ExpressionEngine {

    public static final String VAR_ROLL_INDEX = "roll_index";
    public static final String VAR_BUNDLE_SIZE = "bundle_size";
    public static final String VAR_HISTORY_SIZE = "history_size";
    public static final String VAR_SOURCE = "source";

    private static final int MAX_EXPRESSION_LENGTH = 512;
    /** Namespace prefix for built-in helper functions; call as {@code fn:clamp(...)}. */
    public static final String FN_NAMESPACE = "fn";
    private static final Map<String, Object> NAMESPACES = Map.of(FN_NAMESPACE, new LootFunctions());

    private static final JexlEngine ENGINE = new JexlBuilder()
            .features(new JexlFeatures()
                    .script(false)
                    .loops(false)
                    .lambda(false)
                    .newInstance(false)
                    .sideEffectGlobal(false)
                    .sideEffect(false)
                    .annotation(false)
                    .pragma(false))
            .permissions(JexlPermissions.RESTRICTED.compose("me.mykindos.betterpvp.core.loot.expression.*"))
            .namespaces(NAMESPACES)
            .silent(true)
            .strict(false)
            .safe(true)
            .cache(0) // we cache externally
            .create();

    private static final Cache<String, JexlExpression> CACHE = Caffeine.newBuilder()
            .maximumSize(512)
            .build();

    private ExpressionEngine() {
    }

    /**
     * Evaluates {@code expr} against {@code context}'s inputs plus {@code extras}.
     *
     * @return the result, or {@code null} if the expression failed to parse or evaluate.
     */
    public static @Nullable Object eval(@NotNull String expr, @NotNull LootContext context, @NotNull Map<String, Object> extras) {
        final JexlExpression compiled = compile(expr);
        if (compiled == null) return null;

        final MapContext jc = new MapContext();
        for (Map.Entry<String, Object> e : context.getInputs().entrySet()) {
            jc.set(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Object> e : extras.entrySet()) {
            jc.set(e.getKey(), e.getValue());
        }
        jc.set(VAR_SOURCE, context.getSource().getId());
        jc.set(VAR_HISTORY_SIZE, context.getSession().getProgress().getHistory().size());

        try {
            return compiled.evaluate(jc);
        } catch (Exception ex) {
            log.warn("Failed to evaluate loot expression '{}': {}", expr, ex.getMessage(), ex).submit();
            return null;
        }
    }

    /**
     * Evaluates {@code expr} as a number. Returns {@code fallback} if evaluation fails
     * or the result is not numeric.
     */
    public static double evalDouble(@NotNull String expr, @NotNull LootContext context, @NotNull Map<String, Object> extras, double fallback) {
        final Object r = eval(expr, context, extras);
        if (r instanceof Number n) return n.doubleValue();
        if (r instanceof Boolean b) return b ? 1.0 : 0.0;
        return fallback;
    }

    /**
     * Evaluates {@code expr} as a boolean. {@code null}/non-boolean numeric results follow
     * the rule "non-zero is true". Returns {@code fallback} when evaluation fails entirely.
     */
    public static boolean evalBoolean(@NotNull String expr, @NotNull LootContext context, @NotNull Map<String, Object> extras, boolean fallback) {
        final Object r = eval(expr, context, extras);
        if (r instanceof Boolean b) return b;
        if (r instanceof Number n) return n.doubleValue() != 0.0;
        if (r instanceof String s) return !s.isEmpty();
        return fallback;
    }

    private static @Nullable JexlExpression compile(@NotNull String expr) {
        if (expr.length() > MAX_EXPRESSION_LENGTH) {
            log.warn("Loot expression exceeds {} chars, rejecting", MAX_EXPRESSION_LENGTH).submit();
            return null;
        }
        return CACHE.get(expr, key -> {
            try {
                return ENGINE.createExpression(key);
            } catch (Exception ex) {
                log.warn("Failed to compile loot expression '{}': {}", key, ex.getMessage()).submit();
                return null;
            }
        });
    }

    /**
     * Convenience overload with no extras.
     */
    public static @Nullable Object eval(@NotNull String expr, @NotNull LootContext context) {
        return eval(expr, context, Collections.emptyMap());
    }
}
